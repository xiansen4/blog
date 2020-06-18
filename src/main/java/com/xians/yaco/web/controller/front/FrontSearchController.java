package com.xians.yaco.web.controller.front;


import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import com.xians.yaco.model.domain.Post;
import com.xians.yaco.model.enums.BlogPropertiesEnum;
import com.xians.yaco.model.enums.PostStatusEnum;
import com.xians.yaco.model.enums.PostTypeEnum;
import com.xians.yaco.service.PostService;
import com.xians.yaco.web.controller.core.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.xians.yaco.model.dto.HaloConst.OPTIONS;
import static org.springframework.data.domain.Sort.Direction.DESC;

/**
 * <pre>
 *     文章检索
 * </pre>
 *
 * @author : XIANS
 */
@Slf4j
@Controller
@RequestMapping(value = "/search")
public class FrontSearchController extends BaseController {

    @Autowired
    private PostService postService;

    /**
     * 文章检索
     *
     * @param model   model
     * @param keyword 关键词
     * @return 模板路径/themes/{theme}/search
     */
    @GetMapping
    public String search(Model model,
                         @RequestParam(value = "keyword") String keyword) {
        return this.search(model, HtmlUtil.escape(keyword), 1, Sort.by(DESC, "postDate"));
    }

    /**
     * 文章检索 分页
     *
     * @param model   model
     * @param keyword 关键词
     * @param page    当前页码
     * @return 模板路径/themes/{theme}/search
     */
    @GetMapping(value = "page/{page}")
    public String search(Model model,
                         @RequestParam(value = "keyword") String keyword,
                         @PathVariable(value = "page") Integer page,
                         @SortDefault(sort = "postDate", direction = DESC) Sort sort) {
        int size = 10;
        if (StrUtil.isNotBlank(OPTIONS.get(BlogPropertiesEnum.INDEX_POSTS.getProp()))) {
            size = Integer.parseInt(OPTIONS.get(BlogPropertiesEnum.INDEX_POSTS.getProp()));
        }
        final Pageable pageable = PageRequest.of(page - 1, size, sort);
        final Page<Post> posts = postService.searchPosts(HtmlUtil.escape(keyword), PostTypeEnum.POST_TYPE_POST.getDesc(), PostStatusEnum.PUBLISHED.getCode(), pageable);
        final int[] rainbow = PageUtil.rainbow(page, posts.getTotalPages(), 3);
        model.addAttribute("is_search", true);
        model.addAttribute("keyword", keyword);
        model.addAttribute("posts", posts);
        model.addAttribute("rainbow", rainbow);
        return this.render("search");
    }
}
