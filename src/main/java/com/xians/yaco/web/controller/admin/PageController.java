package com.xians.yaco.web.controller.admin;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.xians.yaco.model.domain.Gallery;
import com.xians.yaco.model.domain.Link;
import com.xians.yaco.model.domain.Post;
import com.xians.yaco.model.domain.User;
import com.xians.yaco.model.dto.JsonResult;
import com.xians.yaco.model.dto.LogsRecord;
import com.xians.yaco.model.enums.BlogPropertiesEnum;
import com.xians.yaco.model.enums.PostTypeEnum;
import com.xians.yaco.model.enums.ResultCodeEnum;
import com.xians.yaco.service.GalleryService;
import com.xians.yaco.service.LinkService;
import com.xians.yaco.service.LogsService;
import com.xians.yaco.service.PostService;
import com.xians.yaco.utils.HaloUtils;
import com.xians.yaco.utils.LocaleMessageUtil;
import com.xians.yaco.utils.MarkdownUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;


import static com.xians.yaco.model.dto.HaloConst.OPTIONS;
import static com.xians.yaco.model.dto.HaloConst.USER_SESSION_KEY;

/**
 * <pre>
 *     后台页面管理控制器
 * </pre>
 *
 * @author : XIANS
 */
@Slf4j
@Controller
@RequestMapping(value = "/admin/page")
public class PageController {

    @Autowired
    LocaleMessageUtil localeMessageUtil;

    @Autowired
    private LinkService linkService;

    @Autowired
    private GalleryService galleryService;

    @Autowired
    private PostService postService;

    @Autowired
    private LogsService logsService;

    @Autowired
    private HttpServletRequest request;

    /**
     * 页面管理页面
     *
     * @param model model
     * @return 模板路径admin/admin_page
     */
    @GetMapping
    public String pages(Model model) {
        final List<Post> posts = postService.findAll(PostTypeEnum.POST_TYPE_PAGE.getDesc());
        model.addAttribute("pages", posts);
        return "admin/admin_page";
    }

    /**
     * 获取友情链接列表并渲染页面
     *
     * @return 模板路径admin/admin_page_link
     */
    @GetMapping(value = "/links")
    public String links() {
        return "admin/admin_page_link";
    }

    /**
     * 跳转到修改页面
     *
     * @param model  model
     * @param linkId linkId 友情链接编号
     * @return String 模板路径admin/admin_page_link
     */
    @GetMapping(value = "/links/edit")
    public String toEditLink(Model model, @RequestParam("linkId") Long linkId) {
        final Optional<Link> link = linkService.fetchById(linkId);
        model.addAttribute("updateLink", link.orElse(new Link()));
        return "admin/admin_page_link";
    }

    /**
     * 处理添加/修改友链的请求并渲染页面
     *
     * @param link Link实体
     * @return JsonResult
     */
    @PostMapping(value = "/links/save")
    @ResponseBody
    public JsonResult saveLink(@Valid Link link, BindingResult result) {
        if (result.hasErrors()) {
            for (ObjectError error : result.getAllErrors()) {
                return new JsonResult(ResultCodeEnum.FAIL.getCode(), error.getDefaultMessage());
            }
        }
        link = linkService.create(link);
        if (null == link) {
            return new JsonResult(ResultCodeEnum.FAIL.getCode(), localeMessageUtil.getMessage("code.admin.common.save-failed"));
        }
        return new JsonResult(ResultCodeEnum.SUCCESS.getCode(), localeMessageUtil.getMessage("code.admin.common.save-success"));
    }

    /**
     * 处理删除友情链接的请求并重定向
     *
     * @param linkId 友情链接编号
     * @return 重定向到/admin/page/links
     */
    @GetMapping(value = "/links/remove")
    public String removeLink(@RequestParam("linkId") Long linkId) {
        try {
            linkService.removeById(linkId);
        } catch (Exception e) {
            log.error("Deleting a friendship link failed: {}", e.getMessage());
        }
        return "redirect:/admin/page/links";
    }

    /**
     * 图库管理
     *
     * @param model model
     * @return 模板路径admin/admin_page_gallery
     */
    @GetMapping(value = "/galleries")
    public String gallery(Model model,
                          @PageableDefault(size = 18, sort = "galleryId", direction = Sort.Direction.DESC) Pageable pageable) {
        final Page<Gallery> galleries = galleryService.listAll(pageable);
        model.addAttribute("galleries", galleries);
        return "admin/admin_page_gallery";
    }

    /**
     * 保存图片
     *
     * @param gallery gallery
     * @return 重定向到/admin/page/gallery
     */
    @PostMapping(value = "/gallery/save")
    public String saveGallery(@ModelAttribute Gallery gallery) {
        try {
            if (StrUtil.isEmpty(gallery.getGalleryThumbnailUrl())) {
                gallery.setGalleryThumbnailUrl(gallery.getGalleryUrl());
            }
            galleryService.create(gallery);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/admin/page/galleries";
    }

    /**
     * 处理获取图片详情的请求
     *
     * @param model     model
     * @param galleryId 图片编号
     * @return 模板路径admin/widget/_gallery-detail
     */
    @GetMapping(value = "/gallery")
    public String gallery(Model model, @RequestParam("galleryId") Long galleryId) {
        final Optional<Gallery> gallery = galleryService.fetchById(galleryId);
        model.addAttribute("gallery", gallery.orElse(new Gallery()));
        return "admin/widget/_gallery-detail";
    }

    /**
     * 删除图库中的图片
     *
     * @param galleryId 图片编号
     * @return JsonResult
     */
    @GetMapping(value = "/gallery/remove")
    @ResponseBody
    public JsonResult removeGallery(@RequestParam("galleryId") Long galleryId) {
        try {
            galleryService.removeById(galleryId);
        } catch (Exception e) {
            log.error("Failed to delete image: {}", e.getMessage());
            return new JsonResult(ResultCodeEnum.FAIL.getCode(), localeMessageUtil.getMessage("code.admin.common.delete-failed"));
        }
        return new JsonResult(ResultCodeEnum.SUCCESS.getCode(), localeMessageUtil.getMessage("code.admin.common.delete-success"));
    }

    /**
     * 跳转到新建页面
     *
     * @param model model
     * @return 模板路径admin/admin_page_md_editor
     */
    @GetMapping(value = "/new")
    public String newPage(Model model) {
        final List<String> customTpls = HaloUtils.getCustomTpl(OPTIONS.get(BlogPropertiesEnum.THEME.getProp()));
        model.addAttribute("customTpls", customTpls);
        return "admin/admin_page_md_editor";
    }

    /**
     * 发表页面
     *
     * @param post    post
     * @param session session
     */
    @PostMapping(value = "/new/push")
    @ResponseBody
    public JsonResult pushPage(@ModelAttribute Post post, HttpSession session) {
        String msg = localeMessageUtil.getMessage("code.admin.common.save-success");
        try {
            //发表用户
            final User user = (User) session.getAttribute(USER_SESSION_KEY);
            post.setUser(user);
            post.setPostType(PostTypeEnum.POST_TYPE_PAGE.getDesc());
            if (null != post.getPostId()) {
                final Post oldPost = postService.fetchById(post.getPostId()).get();
                if (null == post.getPostDate()) {
                    post.setPostDate(DateUtil.date());
                }
                post.setPostViews(oldPost.getPostViews());
                msg = localeMessageUtil.getMessage("code.admin.common.update-success");
            }
            post.setPostContent(MarkdownUtils.renderMarkdown(post.getPostContentMd()));
            //当没有选择文章缩略图的时候，自动分配一张内置的缩略图
            if (StrUtil.equals(post.getPostThumbnail(), BlogPropertiesEnum.DEFAULT_THUMBNAIL.getProp())) {
                post.setPostThumbnail(OPTIONS.get(BlogPropertiesEnum.BLOG_URL.getProp()) + "/static/halo-frontend/images/thumbnail/thumbnail-" + RandomUtil.randomInt(1, 11) + ".jpg");
            }
            postService.create(post);
            logsService.save(LogsRecord.PUSH_PAGE, post.getPostTitle(), request);
            return new JsonResult(ResultCodeEnum.SUCCESS.getCode(), msg);
        } catch (Exception e) {
            log.error("Save page failed: {}", e.getMessage());
            return new JsonResult(ResultCodeEnum.FAIL.getCode(), localeMessageUtil.getMessage("code.admin.common.save-failed"));
        }
    }

    /**
     * 跳转到修改页面
     *
     * @param pageId 页面编号
     * @param model  model
     * @return admin/admin_page_md_editor
     */
    @GetMapping(value = "/edit")
    public String editPage(@RequestParam("pageId") Long pageId, Model model) {
        final Optional<Post> post = postService.fetchById(pageId);
        final List<String> customTpls = HaloUtils.getCustomTpl(OPTIONS.get(BlogPropertiesEnum.THEME.getProp()));
        model.addAttribute("post", post.orElse(new Post()));
        model.addAttribute("customTpls", customTpls);
        return "admin/admin_page_md_editor";
    }

    /**
     * 检查该路径是否已经存在
     *
     * @param postUrl postUrl
     * @return JsonResult
     */
    @GetMapping(value = "/checkUrl")
    @ResponseBody
    public JsonResult checkUrlExists(@RequestParam("postUrl") String postUrl) {
        final Post post = postService.findByPostUrl(postUrl, PostTypeEnum.POST_TYPE_PAGE.getDesc());
        if (null != post) {
            return new JsonResult(ResultCodeEnum.FAIL.getCode(), localeMessageUtil.getMessage("code.admin.common.url-is-exists"));
        }
        return new JsonResult(ResultCodeEnum.SUCCESS.getCode(), "");
    }

    @InitBinder
    public void initBinder(ServletRequestDataBinder binder) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        binder.registerCustomEditor(Date.class, new CustomDateEditor(sdf, true));
    }
}
