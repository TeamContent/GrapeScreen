package interfaceApplication;

import java.util.HashMap;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import interfaceModel.GrapeTreeDBModel;
import model.Model;
import nlogger.nlogger;
import security.codec;
import session.session;
import string.StringHelper;

public class Element {
	private GrapeTreeDBModel gDbModel;
	private Model model;
	private session se;
	private JSONObject userInfo = new JSONObject();
	private String userid = "";
	private String sid = null;

	public Element() {
		gDbModel = new GrapeTreeDBModel();
		gDbModel.form("element").bindApp();
		model = new Model();
		se = new session();
		userInfo = se.getDatas();
		if (userInfo != null && userInfo.size() != 0) {
			userid = userInfo.getMongoID("_id");
		}
		sid = session.getSID();
	}

	/**
	 * 添加默认值
	 * 
	 * @return
	 */
	private HashMap<String, Object> getInitData() {
		HashMap<String, Object> defmap = model.AddFixField();
		// 设置screen表独有的字段
		defmap.put("bid", "0"); // 所属用户id
		defmap.put("content", ""); // 元素内容，jsonarray形式，[{"type":"","content":""}]
		return defmap;
	}

	/**
	 * 新增元素信息
	 * 
	 * @project GrapeScreen
	 * @package interfaceApplication
	 * @file Screen.java
	 * 
	 * @param ScreenInfo
	 * @return
	 *
	 */
	@SuppressWarnings("unchecked")
	public String AddElement(String ElementInfo) {
		Object info = "";
		String content;
		JSONObject obj = model.AddMap(getInitData(), ElementInfo);
		if (obj != null && obj.size() != 0 && obj.containsKey("content")) {
			content = obj.getString("content");
			content = codec.DecodeHtmlTag(content);
			content = codec.decodebase64(content);
			obj.put("content", content);
		}
		gDbModel.checkMode();
		info = gDbModel.data(obj).insertEx();
		if (info == null) {
			return model.resultmsg(1);
		}
		obj = Find(info.toString());
		return model.resultJSONInfo(obj);
	}

	/**
	 * 修改元素信息
	 * 
	 * @project GrapeScreen
	 * @package interfaceApplication
	 * @file Screen.java
	 * 
	 * @param id
	 * @param ScreenInfo
	 * @return
	 *
	 */
	@SuppressWarnings("unchecked")
	public String UpdateElement(String id, String ElementInfo) {
		Theme theme = new Theme();
		String cont;
		String tid = theme.getTidByeid(id);
		JSONObject object = new JSONObject();
		int code = 0;
		JSONObject obj = JSONObject.toJSON(ElementInfo);
		if (obj != null && obj.size() != 0) {
			if (obj.containsKey("content")) {
				cont = obj.getString("content");
				cont = codec.DecodeHtmlTag(cont);
				cont = codec.decodebase64(cont);
				obj.put("content", cont);
			}
			gDbModel.eq("_id", id);
			code = (gDbModel.dataEx(obj).update() != null) ? 0 : 99;
			if (code == 0) {
				if (!tid.equals("")) {
					JSONObject content = Find(id);
					object.put("target", id);
					object.put("content", content);
					broadManage.broadEvent(tid, 1, object.toString());
				}

			}
		}
		return model.resultMessage(code, "修改成功");
	}

	@SuppressWarnings("unchecked")
	protected String UpdateAllElement(JSONArray eleArray) {
		String content;
		JSONObject obj;
		String id;
		if (eleArray != null && eleArray.size() != 0) {
			for (Object object : eleArray) {
				obj = (JSONObject) object;
				if (obj.containsKey("content")) {
					content = obj.getString("content");
					content = codec.DecodeHtmlTag(content);
					content = codec.decodebase64(content);
					obj.put("content", content);
				}
				id = obj.getString("_id");
				obj.remove("_id");
				gDbModel.eq("_id", id).data(obj).update();
			}
		}
		return this.model.resultMessage(0, "修改成功");
	}

	/**
	 * 删除元素信息
	 * 
	 * @project GrapeScreen
	 * @package interfaceApplication
	 * @file Screen.java
	 * 
	 * @param ids
	 * @return
	 *
	 */
	public String DeleteElement(String ids) {
		long tipcode = 0;
		String[] value = ids.split(",");
		int l = value.length;
		gDbModel.or();
		for (String id : value) {
			gDbModel.eq("_id", id);
		}
		tipcode = gDbModel.deleteAll();
		return model.resultMessage(tipcode == l ? 0 : 99, "删除成功");
	}

	/**
	 * 分页显示大屏信息
	 * 
	 * @project GrapeScreen
	 * @package interfaceApplication
	 * @file Screen.java
	 * 
	 * @param idx
	 * @param PageSize
	 * @param condString
	 * @return
	 *
	 */
	public String PageElement(int idx, int PageSize, String condString) {
		JSONArray array = null;
		long total = 0, totalSize = 0;
		JSONArray condArray = JSONArray.toJSONArray(condString);
		if (condArray != null && condArray.size() != 0) {
			gDbModel.where(condArray);
		}
		if (sid != null && sid.equals("")) {
			if (!model.isAdmin()) {
				gDbModel.eq("userid", userid);
			}
			array = gDbModel.mask("itemfatherID,itemSort,deleteable,visable,itemLevel,mMode,uMode,dMode").dirty()
					.page(idx, PageSize);
			total = gDbModel.dirty().count();
			totalSize = gDbModel.pageMax(PageSize);
		}
		return model.pageShow(array, total, totalSize, idx, PageSize);
	}

	/**
	 * 显示所有的元素信息，非管理员用户只能查看与自己相关的大屏信息
	 * 
	 * @project GrapeScreen
	 * @package interfaceApplication
	 * @file Screen.java
	 * 
	 * @param idx
	 * @param PageSize
	 * @param condString
	 * @return
	 *
	 */
	public String ShowScreen() {
		JSONArray array = null;
		if (sid != null && sid.equals("")) {
			if (!model.isAdmin()) {
				gDbModel.eq("userid", userid);
			}
			array = gDbModel.mask("itemfatherID,itemSort,deleteable,visable,itemLevel,mMode,uMode,dMode").select();
		}
		return model.resultArray(array);
	}

	/**
	 * 批量查询元素信息，封装成固定格式输出，{"message":{"records":[]},"errorcode":0}
	 * 
	 * @project GrapeScreen
	 * @package interfaceApplication
	 * @file Block.java
	 * 
	 * @param ids
	 * @return
	 *
	 */
	public String ShowElement(String ids) {
		return model.resultArray(BatchElement(ids));
	}

	/**
	 * 获取元素信息，封装成{eid:{"content":""},eid:{"content":""}}
	 * 
	 * @project GrapeScreen
	 * @package interfaceApplication
	 * @file Block.java
	 * 
	 * @param ids
	 * @return
	 *
	 */
	@SuppressWarnings("unchecked")
	public JSONObject GetElementInfo(String ids) {
		JSONObject tempObj, obj = new JSONObject();
		String id;
		int l = 0;
		JSONArray array = BatchElement(ids);
		JSONArray contentobj = new JSONArray();
		array = getBlocks(array);
		if (array != null && array.size() != 0) {
			l = array.size();
			for (int i = 0; i < l; i++) {
				tempObj = (JSONObject) array.get(i);
				id = tempObj.getMongoID("_id");
				contentobj = JSONArray.toJSONArray(tempObj.get("content").toString());
				tempObj.put("content", (contentobj != null && contentobj.size() != 0) ? contentobj : new JSONArray());
				obj.put(id, tempObj);
			}
		}
		return obj;
	}

	@SuppressWarnings("unchecked")
	public JSONObject GetElementInfos(String ids) {
		JSONObject tempObj, obj = new JSONObject();
		String id;
		int l = 0;
		JSONArray array = BatchElement(ids);
		array = getBlock(array);
		if (array != null && array.size() != 0) {
			l = array.size();
			for (int i = 0; i < l; i++) {
				tempObj = (JSONObject) array.get(i);
				id = tempObj.getMongoID("_id");
				obj.put(id, tempObj);
			}
		}
		return obj;
	}
	/**
	 * 获取区域信息,添加至元素信息中
	 * 
	 * @param array
	 * @return
	 */
	private JSONArray getBlock(JSONArray array) {
		array = new Mode().getBlock(array);
		return array;
	}

	private JSONArray getBlocks(JSONArray array) {
		Block block = new Block();
		String bid = "";
		JSONObject BlockInfo = new JSONObject();
		bid = getBid(array);
		if (bid.length() > 0) {
			BlockInfo = block.GetBlockInfo(bid);
		}
		return Block2Element(array, BlockInfo);
	}

	private String getBid(JSONArray array) {
		String bid = "";
		if ((array != null) && (array.size() != 0)) {
			int l = array.size();
			for (int i = 0; i < l; i++) {
				JSONObject object = (JSONObject) array.get(i);
				String tempId = object.getString("bid");
				bid = bid + tempId + ",";
			}
			if (bid.length() > 0) {
				bid = StringHelper.fixString(bid, ',');
			}
		}
		return bid;
	}

	@SuppressWarnings("unchecked")
	private JSONArray Block2Element(JSONArray array, JSONObject BlockInfo) {
		if ((BlockInfo != null) && (BlockInfo.size() != 0)) {
			int l = array.size();
			for (int i = 0; i < l; i++) {
				JSONObject object = (JSONObject) array.get(i);
				array.set(i, FillBlock(object, BlockInfo));
			}
		}
		return array;
	}

	@SuppressWarnings("unchecked")
	private JSONObject FillBlock(JSONObject ModeInfo, JSONObject BlockInfo) {
		// JSONArray tempArray = new JSONArray();
		String id,area = "",areaid = "";
		if ((ModeInfo != null) && (ModeInfo.size() != 0)) {
			String bid = ModeInfo.getString("bid");
			String[] value = bid.split(",");
			for (String str : value) {
				JSONObject tempObj = (JSONObject) BlockInfo.get(str);
				if ((tempObj != null) && (tempObj.size() != 0)) {
					// tempArray.add(tempObj);
					id = ((JSONObject) tempObj.get("_id")).getString("$oid");
					area = tempObj.getString("area");
					areaid = id;
				}
			}
			ModeInfo.put("area", area);
			ModeInfo.put("areaid", areaid);
			// ModeInfo.remove("bid");
//			 ModeInfo.put("area", tempArray);
		}
		return ModeInfo;
	}
	//
	// private JSONArray getBlock(JSONArray array) {
	// Block block = new Block();
	// JSONObject object;
	// String tempId, bid = "";
	// if (array != null && array.size() != 0) {
	// int l = array.size();
	// for (int i = 0; i < l; i++) {
	// object = (JSONObject) array.get(i);
	// tempId = object.getString("bid");
	// bid += tempId + ",";
	// }
	// if (bid.length() > 0) {
	// bid = StringHelper.fixString(bid, ',');
	// }
	// if (bid.length() > 0) {
	// array = block.Block2Mode(bid, array);
	// }
	// }
	// return array;
	// }

	/**
	 * 批量查询元素信息
	 * 
	 * @project GrapeScreen
	 * @package interfaceApplication
	 * @file Block.java
	 * 
	 * @param ids
	 * @return
	 *
	 */
	private JSONArray BatchElement(String ids) {
		gDbModel.or();
		JSONArray array = null;
		if (ids != null && !ids.equals("") && !ids.equals("null")) {
			String[] value = ids.split(",");
			if (value.length > 0) {
				for (String id : value) {
					if (ObjectId.isValid(id)) {
						gDbModel.eq("_id", id);
					}
				}
				array = gDbModel.mask("itemfatherID,itemSort,deleteable,visable,itemLevel,mMode,uMode,dMode").select();
			}
		}
		return array;
	}

	/**
	 * 显示详细信息
	 * 
	 * @project GrapeScreen
	 * @package interfaceApplication
	 * @file Screen.java
	 * 
	 * @param info
	 * @return
	 *
	 */
	public JSONObject Find(String info) {
		JSONObject obj = gDbModel.eq("_id", info).field("_id,content,bid").limit(1).find();
		return obj;
	}

	/**
	 * 添加元素信息到主题信息数据中
	 * 
	 * @project GrapeScreen
	 * @package interfaceApplication
	 * @file Mode.java
	 * 
	 * @param array
	 * @param ElementInfo
	 * @return
	 *
	 */
	@SuppressWarnings("unchecked")
	public JSONArray Element2Themem(String eid, JSONArray array) {
		JSONObject tempObj = new JSONObject();
		JSONObject ElementInfo;
		if (eid != null && !eid.equals("")) {
			ElementInfo = GetElementInfo(eid);
			if (ElementInfo != null && ElementInfo.size() != 0) {
				int l = array.size();
				for (int i = 0; i < l; i++) {
					tempObj = (JSONObject) array.get(i);
					tempObj = FillElement(tempObj, ElementInfo);
					array.set(i, tempObj);
				}
			}
		}
		return array;
	}

	/**
	 * 元素内容填充至主题中
	 * 
	 * @project GrapeScreen
	 * @package interfaceApplication
	 * @file Theme.java
	 * 
	 * @param ThemeInfo
	 * @param ElementInfo
	 * @return
	 *
	 */
	@SuppressWarnings("unchecked")
	public JSONObject FillElement(JSONObject ThemeInfo, JSONObject ElementInfo) {
		JSONObject tempObj;
		JSONArray tempArray = new JSONArray(), contentArray;
		String bid;
		String[] value;
		if (ThemeInfo != null && ThemeInfo.size() != 0) {
			bid = ThemeInfo.getString("content");
			value = bid.split(",");
			for (String str : value) {
				tempObj = (JSONObject) ElementInfo.get(str);
				if (tempObj != null && tempObj.size() != 0) {
					contentArray = JSONArray.toJSONArray(tempObj.getString("content"));
					tempObj.put("content", contentArray);
					tempArray.add(tempObj);
				}
			}
			ThemeInfo.remove("content");
			ThemeInfo.put("element", tempArray);
		}
		return ThemeInfo;
	}
}