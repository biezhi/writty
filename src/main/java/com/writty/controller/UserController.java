package com.writty.controller;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.blade.Blade;
import com.blade.ioc.annotation.Inject;
import com.blade.jdbc.Page;
import com.blade.route.annotation.Path;
import com.blade.route.annotation.Route;
import com.blade.view.ModelAndView;
import com.blade.web.http.HttpMethod;
import com.blade.web.http.Request;
import com.blade.web.http.Response;
import com.blade.web.multipart.FileItem;
import com.writty.Constant;
import com.writty.kit.SessionKit;
import com.writty.kit.Utils;
import com.writty.model.Post;
import com.writty.model.User;
import com.writty.service.FavoriteService;
import com.writty.service.PostService;
import com.writty.service.UserService;

import blade.kit.DateKit;
import blade.kit.EncrypKit;
import blade.kit.FileKit;
import blade.kit.PatternKit;
import blade.kit.StringKit;
import blade.kit.json.JSONObject;

@Path("/")
public class UserController extends BaseController {
	
	@Inject
	private UserService userService;
	
	@Inject
	private PostService postService;
	
	@Inject
	private FavoriteService favoriteService;
	
	/**
	 * 我的文章列表
	 */
	@Route(value = "/my", method = HttpMethod.GET)
	public ModelAndView my_articles(Request request, Response response){
		User user = SessionKit.getLoginUser();
		if(null == user){
			response.go("/");
			return null;
		}
		
		Integer page = request.queryAsInt("p");
		
		// 未审核列表
		List<Post> auditList = postService.getList(user.getUid(), 0, null, null);
		request.attribute("auditList", auditList);
		
		// 我发布的
		Page<Map<String, Object>> publishPage = postService.getPageListMap(user.getUid(), null, null, null, page, 10, "is_pub asc, created desc");
		request.attribute("publishPage", publishPage);
		
		// 我收藏的
		Page<Map<String, Object>> favoritePage = favoriteService.getMyFavorites(user.getUid(), page, 10);
		request.attribute("favoritePage", favoritePage);
				
		return this.getView("my");
	}
	
	/**
	 * 我的收藏
	 */
	@Route(value = "/favorites", method = HttpMethod.GET)
	public ModelAndView show_favorites(Request request, Response response){
		User user = SessionKit.getLoginUser();
		if(null == user){
			response.go("/");
			return null;
		}
		
		Integer page = request.queryAsInt("p");
		Page<Map<String, Object>> favoritePage = favoriteService.getMyFavorites(user.getUid(), page, count);
		request.attribute("favoritePage", favoritePage);
		
		return this.getView("favorites");
	}
	
	/**
	 * 收藏文章
	 */
	@Route(value = "/favorite", method = HttpMethod.POST)
	public void favorite(Request request, Response response){
		User user = SessionKit.getLoginUser();
		if(null == user){
			this.nosignin(response);
			return;
		}
		String pid = request.query("pid");
		String type = request.query("type");
		
		boolean flag = false;
		if(type.equals("favorite")){
			flag = favoriteService.favorite(user.getUid(), pid);
		}
		if(type.equals("unfavorite")){
			flag = favoriteService.delete(user.getUid(), pid);
		}
		if(flag){
			this.success(response, "");
		} else {
			this.error(response, "收藏失败");
		}
	}
	
	/**
	 * 个人设置页面
	 */
	@Route(value = "/settings", method = HttpMethod.GET)
	public ModelAndView show_settings(Request request, Response response){
		User user = SessionKit.getLoginUser();
		if(null == user){
			response.go("/");
			return null;
		}
		return this.getView("settings");
	}
	
	/**
	 * 申请编辑页面
	 */
	@Route(value = "/request", method = HttpMethod.GET)
	public ModelAndView show_request(Request request, Response response){
		User user = SessionKit.getLoginUser();
		if(null == user){
			response.go("/");
			return null;
		}
		return this.getView("request");
	}
	
	/**
	 * 申请编辑
	 */
	@Route(value = "/request", method = HttpMethod.POST)
	public void requestEditor(Request request, Response response){
		User user = SessionKit.getLoginUser();
		if(null == user){
			this.nosignin(response);
			return;
		}
		
		// github帐号
		String github = request.query("github");
		// 联系邮箱
		String email = request.query("email");
		// 个人介绍
		String intro = request.query("intro");
		
		if(StringKit.isBlank(github) || StringKit.isBlank(email) ||
				StringKit.isBlank(intro)){
			this.error(response, "参数不能为空");
			return;
		}
		
		if(!Utils.isEmail(email)){
			this.error(response, "邮箱格式错误");
			return;
		}
		
		if(intro.length() < 50){
			this.error(response, "请完善您的个人介绍");
			return;
		}
		
	}
	
	/**
	 * 修改密码
	 */
	@Route(value = "/reset_pwd", method = HttpMethod.POST)
	public void reset_pwd(Request request, Response response){
		User user = SessionKit.getLoginUser();
		if(null == user){
			response.go("/");
			return;
		}
		String curpwd = request.query("curpwd");
		if(StringKit.isNotBlank(curpwd)){
			if(!user.getPass_word().equals(EncrypKit.md5(user.getUser_name() + curpwd))){
				this.error(response, "当前密码不正确");
				return;
			}
		}
		
		String pwd = request.query("newpwd");
		if(StringKit.isBlank(pwd) || pwd.length() < 6 || pwd.length() > 20){
			this.error(response, "请输入6至20位密码");
			return;
		}
		
		String newpwd = EncrypKit.md5(user.getUser_name() + pwd);
		userService.updatePwd(user.getUid(), newpwd);
		this.success(response, "");
	}
	
	/**
	 * 上传头像
	 */
	@Route(value = "/uploadimg", method = HttpMethod.POST)
	public void uploadimg(Request request, Response response){
		User user = SessionKit.getLoginUser();
		if(null == user){
			return;
		}
		
		FileItem[] fileItems = request.files();
		if(null != fileItems && fileItems.length > 0){
			
			FileItem fileItem = fileItems[0];
			
			String type = request.query("type");
			String suffix = FileKit.getExtension(fileItem.getFileName());
			if(StringKit.isNotBlank(suffix)){
				suffix = "." + suffix;
			}
			if(!PatternKit.isImage(suffix)){
				return;
			}
			
			if(null == type){
				type = "temp";
			}
			
			String saveName = DateKit.dateFormat(new Date(), "yyyyMMddHHmmssSSS")  + "_" + StringKit.getRandomChar(10) + suffix;
			File file = new File(Blade.me().webRoot() + File.separator + Constant.UPLOAD_FOLDER + File.separator + saveName);
			
			try {
				
				Utils.copyFileUsingFileChannels(fileItem.getFile(), file);
				
				String filePath = Constant.UPLOAD_FOLDER + "/" + saveName;
				
				JSONObject res = new JSONObject();
				res.put("status", 200);
				res.put("savekey", filePath);
				res.put("savepath", filePath);
				res.put("url", Constant.SITE_URL + "/" + filePath);
				
				response.json(res.toString());
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}
}
