package com.xians.yaco.web.controller.admin;


import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xians.yaco.model.domain.*;
import com.xians.yaco.model.dto.JsonResult;
import com.xians.yaco.model.dto.LogsRecord;
import com.xians.yaco.model.enums.*;
import com.xians.yaco.service.*;
import com.xians.yaco.utils.LocaleMessageUtil;
import com.xians.yaco.utils.MarkdownUtils;
import com.xians.yaco.web.controller.core.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.xians.yaco.model.dto.HaloConst.OPTIONS;
import static com.xians.yaco.model.dto.HaloConst.USER_SESSION_KEY;


/**
 * <pre>
 *     后台首页控制器
 * </pre>
 *
 * @author : XIANS
 */
@Slf4j
@Controller
@RequestMapping(value = "/admin")
public class AdminController extends BaseController {

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @Autowired
    private LogsService logsService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private CommentService commentService;

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TagService tagService;

    @Autowired
    private OptionsService optionsService;

    @Autowired
    private GalleryService galleryService;

    @Autowired
    private LinkService linkService;

    @Autowired
    private MenuService menuService;

    @Autowired
    private LocaleMessageUtil localeMessageUtil;

    /**
     * 请求后台页面
     *
     * @param model model
     * @return 模板路径admin/admin_index
     */
    @GetMapping(value = {"", "/index"})
    public String index(Model model) {

        //查询评论的条数
        final Long commentCount = commentService.count();
        model.addAttribute("commentCount", commentCount);

        //查询最新的文章
        final List<Post> postsLatest = postService.findPostLatest();
        model.addAttribute("postTopFive", postsLatest);

        //查询最新的日志
        final List<Logs> logsLatest = logsService.findLogsLatest();
        model.addAttribute("logs", logsLatest);

        //查询最新的评论
        final List<Comment> comments = commentService.findCommentsLatest();
        model.addAttribute("comments", comments);

        //附件数量
        model.addAttribute("mediaCount", attachmentService.count());

        //文章阅读总数
        final Long postViewsSum = postService.getPostViews();
        model.addAttribute("postViewsSum", postViewsSum);

        //成立天数
        final Date blogStart = DateUtil.parse(OPTIONS.get(BlogPropertiesEnum.BLOG_START.getProp()));
        final long hadDays = DateUtil.between(blogStart, DateUtil.date(), DateUnit.DAY);
        model.addAttribute("hadDays", hadDays);
        return "admin/admin_index";
    }

    /**
     * 处理跳转到登录页的请求
     *
     * @param session session
     * @return 模板路径admin/admin_login
     */
    @GetMapping(value = "/login")
    public String login(HttpSession session) {
        final User user = (User) session.getAttribute(USER_SESSION_KEY);
        //如果session存在，跳转到后台首页
        if (null != user) {
            return "redirect:/admin";
        }
        return "admin/admin_login";
    }

    /**
     * 验证登录信息
     *
     * @param loginName 登录名：邮箱／用户名
     * @param loginPwd  loginPwd 密码
     * @param session   session session
     * @return JsonResult JsonResult
     */
    @PostMapping(value = "/getLogin")
    @ResponseBody
    public JsonResult getLogin(@ModelAttribute("loginName") String loginName,
                               @ModelAttribute("loginPwd") String loginPwd,
                               HttpSession session) {
        //已注册账号，单用户，只有一个
        final User aUser = userService.findUser();
        //首先判断是否已经被禁用已经是否已经过了10分钟
        Date loginLast = DateUtil.date();
        if (null != aUser.getLoginLast()) {
            loginLast = aUser.getLoginLast();
        }
        final Long between = DateUtil.between(loginLast, DateUtil.date(), DateUnit.MINUTE);
        if (StrUtil.equals(aUser.getLoginEnable(), TrueFalseEnum.FALSE.getDesc()) && (between < CommonParamsEnum.TEN.getValue())) {
            return new JsonResult(ResultCodeEnum.FAIL.getCode(), localeMessageUtil.getMessage("code.admin.login.disabled"));
        }
        //验证用户名和密码
        User user = null;
        if (Validator.isEmail(loginName)) {
            user = userService.userLoginByEmail(loginName, SecureUtil.md5(loginPwd));
        } else {
            user = userService.userLoginByName(loginName, SecureUtil.md5(loginPwd));
        }
        userService.updateUserLoginLast(DateUtil.date());
        //判断User对象是否相等
        if (ObjectUtil.equal(aUser, user)) {
            session.setAttribute(USER_SESSION_KEY, aUser);
            //重置用户的登录状态为正常
            userService.updateUserNormal();
            //保存当前用户的状态
            logsService.save(LogsRecord.LOGIN, LogsRecord.LOGIN_SUCCESS, request);
            //后台打印登录信息
            log.info("User {} login succeeded.", aUser.getUserDisplayName());
            //将用户的数据返回和记得国际化的状态
            return new JsonResult(ResultCodeEnum.SUCCESS.getCode(), localeMessageUtil.getMessage("code.admin.login.success"));
        } else {
            //更新失败次数
            final Integer errorCount = userService.updateUserLoginError();
            //超过五次禁用账户
            if (errorCount >= CommonParamsEnum.FIVE.getValue()) {
                userService.updateUserLoginEnable(TrueFalseEnum.FALSE.getDesc());
            }
            logsService.save(LogsRecord.LOGIN, LogsRecord.LOGIN_ERROR + "[" + HtmlUtil.escape(loginName) + "," + HtmlUtil.escape(loginPwd) + "]", request);
            final Object[] args = {(5 - errorCount)};
            return new JsonResult(ResultCodeEnum.FAIL.getCode(), localeMessageUtil.getMessage("code.admin.login.failed", args));
        }
    }

    /**
     * 退出登录 销毁session
     *
     * @param session session
     * @return 重定向到/admin/login
     */
    @GetMapping(value = "/logOut")
    public String logOut(HttpSession session) {
        //得到之前用户登录后存放得session
        final User user = (User) session.getAttribute(USER_SESSION_KEY);
        //销毁session
        session.removeAttribute(USER_SESSION_KEY);
        //保存当前用户的状态
        logsService.save(LogsRecord.LOGOUT, user.getUserName(), request);
        //后台打印用户的已经登出账号及用户的用户名
        log.info("User {} has logged out", user.getUserName());
        return "redirect:/admin/login";
    }

    /**
     * 查看所有日志
     *
     * @param model model model
     * @return 模板路径admin/widget/_logs-all
     */
    @GetMapping(value = "/logs")
    public String logs(Model model, @PageableDefault Pageable pageable) {
        //查询保存的所有日志文件
        final Page<Logs> logs = logsService.listAll(pageable);
        //得到的的数据传递给前端
        model.addAttribute("logs", logs);
        return "admin/widget/_logs-all";
    }

    /**
     * 清除所有日志
     *
     * @return 重定向到/admin
     */
    @GetMapping(value = "/logs/clear")
    public String logsClear() {
        try {
            //将所有的的日志全部删除
            logsService.removeAll();
        } catch (Exception e) {
            //后台打印日志
            log.error("Clear log failed:{}" + e.getMessage());
        }
        return "redirect:/admin";
    }

    /**
     * Halo关于页面
     *
     * @return 模板路径admin/admin_halo
     */
//    @GetMapping(value = "/halo")
//    public String halo() {
//        return "admin/admin_halo";
//    }

    /**
     * 获取一个Token
     *
     * @return JsonResult
     */
    @GetMapping(value = "/getToken")
    @ResponseBody
    public JsonResult getToken() {
        //获取令牌
        final String token = (System.currentTimeMillis() + new Random().nextInt(999999999)) + "";
        //得到获取的令牌
        return new JsonResult(ResultCodeEnum.SUCCESS.getCode(), HttpStatus.OK.getReasonPhrase(), SecureUtil.md5(token));
    }

    /**
     * 小工具
     *
     * @return String
     */
    @GetMapping(value = "/tools")
    public String tools() {
        return "admin/admin_tools";
    }

    /**
     * Markdown 导入页面
     *
     * @return String
     */
    @GetMapping(value = "/tools/markdownImport")
    public String markdownImport() {
        return "admin/widget/_markdown_import";
    }

    /**
     * Markdown 导入
     *
     * @param file    file
     * @param request request
     * @return JsonResult
     */
    @PostMapping(value = "/tools/markdownImport")
    @ResponseBody
    public JsonResult markdownImport(@RequestParam("file") MultipartFile file,
                                     HttpServletRequest request,
                                     HttpSession session) throws IOException {
        //得到存储的session的值
        final User user = (User) session.getAttribute(USER_SESSION_KEY);
        //得到Markdown的输入流及设置字符的编码
        final String markdown = IoUtil.read(file.getInputStream(), "UTF-8");
        //得到输入的内容,将Markdown的内容渲染到页面
        final String content = MarkdownUtils.renderMarkdown(markdown);
        //
        final Map<String, List<String>> frontMatters = MarkdownUtils.getFrontMatter(markdown);
        final Post post = new Post();
        List<String> elementValue = null;
        final List<Tag> tags = new ArrayList<>();
        final List<Category> categories = new ArrayList<>();
        Tag tag = null;
        Category category = null;
        if (frontMatters.size() > 0) {
            for (String key : frontMatters.keySet()) {
                elementValue = frontMatters.get(key);
                for (String ele : elementValue) {
                    if ("title".equals(key)) {
                        post.setPostTitle(ele);
                    } else if ("date".equals(key)) {
                        post.setPostDate(DateUtil.parse(ele));
                    } else if ("updated".equals(key)) {
                        post.setPostUpdate(DateUtil.parse(ele));
                    } else if ("tags".equals(key)) {
                        tag = tagService.findTagByTagName(ele);
                        if (null == tag) {
                            tag = new Tag();
                            tag.setTagName(ele);
                            tag.setTagUrl(ele);
                            tag = tagService.create(tag);
                        }
                        tags.add(tag);
                    } else if ("categories".equals(key)) {
                        category = categoryService.findByCateName(ele);
                        if (null == category) {
                            category = new Category();
                            category.setCateName(ele);
                            category.setCateUrl(ele);
                            category.setCateDesc(ele);
                            category = categoryService.create(category);
                        }
                        categories.add(category);
                    }
                }
            }
        } else {
            post.setPostDate(new Date());
            post.setPostUpdate(new Date());
            post.setPostTitle(file.getOriginalFilename());
        }
        post.setPostContentMd(markdown);
        post.setPostContent(content);
        post.setPostType(PostTypeEnum.POST_TYPE_POST.getDesc());
        post.setAllowComment(AllowCommentEnum.ALLOW.getCode());
        post.setUser(user);
        post.setTags(tags);
        post.setCategories(categories);
        post.setPostUrl(StrUtil.removeSuffix(file.getOriginalFilename(), ".md"));
        if (null == post.getPostDate()) {
            post.setPostDate(new Date());
        }
        if (null == post.getPostUpdate()) {
            post.setPostUpdate(new Date());
        }
        postService.create(post);
        return new JsonResult(ResultCodeEnum.SUCCESS.getCode());
    }


    /**
     * 导出博客数据
     *
     * @param response response
     */
    @GetMapping(value = "/tools/dataExport")
    @ResponseBody
    public void dataExport(HttpServletResponse response) {
        final Map<String, String> options = optionsService.findAllOptions();
        final List<Attachment> attachments = attachmentService.listAll();
        final List<Post> posts = postService.listAll();
        final List<Gallery> galleries = galleryService.listAll();
        final List<Link> links = linkService.listAll();
        final List<Menu> menus = menuService.listAll();
        JSONObject data = new JSONObject();
        JSONArray postsJar = new JSONArray();
        for (Post post : posts) {
            JSONObject postObj = new JSONObject();
            postObj.put("postId", post.getPostId());
            postObj.put("postTitle", post.getPostTitle());
            postObj.put("postType", post.getPostType());
            postObj.put("postContentMd", post.getPostContentMd());
            postObj.put("postContent", post.getPostContent());
            postObj.put("postUrl", post.getPostUrl());
            postObj.put("postSummary", post.getPostSummary());
            postObj.put("postThumbnail", post.getPostThumbnail());
            postObj.put("postDate", post.getPostDate());
            postObj.put("postUpdate", post.getPostUpdate());
            postObj.put("postStatus", post.getPostStatus());
            postObj.put("postViews", post.getPostViews());
            postObj.put("allowComment", post.getAllowComment());
            postObj.put("postPassword", post.getPostPassword());
            postObj.put("customTpl", post.getCustomTpl());
            if (null != post.getTags() && post.getTags().size() > 0) {
                JSONArray tagsJar = new JSONArray();
                for (Tag tag : post.getTags()) {
                    JSONObject tagObj = new JSONObject();
                    tagObj.put("tagId", tag.getTagId());
                    tagObj.put("tagName", tag.getTagName());
                    tagObj.put("tagUrl", tag.getTagUrl());
                    tagsJar.add(tagObj);
                }
                postObj.put("tags", tagsJar);
            }

            if (null != post.getCategories() && post.getCategories().size() > 0) {
                JSONArray categoriesJar = new JSONArray();
                for (Category category : post.getCategories()) {
                    JSONObject categoryObj = new JSONObject();
                    categoryObj.put("cateId", category.getCateId());
                    categoryObj.put("cateName", category.getCateName());
                    categoryObj.put("cateUrl", category.getCateUrl());
                    categoryObj.put("cateDesc", category.getCateDesc());
                    categoriesJar.add(categoryObj);
                }
                postObj.put("categories", categoriesJar);
            }
            if (null != post.getComments() && post.getComments().size() > 0) {
                JSONArray commentsJar = new JSONArray();
                for (Comment comment : post.getComments()) {
                    JSONObject commentObj = new JSONObject();
                    commentObj.put("commentId", comment.getCommentId());
                    commentObj.put("commentAuthor", comment.getCommentAuthor());
                    commentObj.put("commentAuthorEmail", comment.getCommentAuthorEmail());
                    commentObj.put("commentAuthorUrl", comment.getCommentAuthorUrl());
                    commentObj.put("commentAuthorIp", comment.getCommentAuthorIp());
                    commentObj.put("commentAuthorAvatarMd5", comment.getCommentAuthorAvatarMd5());
                    commentObj.put("commentContent", comment.getCommentContent());
                    commentObj.put("commentAgent", comment.getCommentAgent());
                    commentObj.put("commentParent", comment.getCommentParent());
                    commentObj.put("commentStatus", comment.getCommentStatus());
                    commentObj.put("isAdmin", comment.getIsAdmin());
                    commentObj.put("createDate",comment.getCommentDate());
                    commentsJar.add(commentObj);
                }
                postObj.put("comments", commentsJar);
            }
            postsJar.add(postObj);
        }
        data.put("options", JSONUtil.parseFromMap(options));
        data.put("attachments", JSONUtil.parseArray(attachments));
        data.put("posts", postsJar);
        data.put("galleries", JSONUtil.parseArray(galleries));
        data.put("links", JSONUtil.parseArray(links));
        data.put("menus", JSONUtil.parseArray(menus));

        response.setContentType("application/octet-stream");
        response.setHeader("Content-disposition", "attachment;filename=halo_data.json");

        BufferedOutputStream bufferedOutputStream = null;
        ServletOutputStream servletOutputStream = null;
        try {
            servletOutputStream = response.getOutputStream();
            bufferedOutputStream = new BufferedOutputStream(servletOutputStream);
            bufferedOutputStream.write(data.toString().getBytes(StandardCharsets.UTF_8));
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != bufferedOutputStream) {
                    bufferedOutputStream.close();
                }
                if (null != servletOutputStream) {
                    servletOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
