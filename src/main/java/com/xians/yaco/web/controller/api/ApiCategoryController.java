package com.xians.yaco.web.controller.api;


import com.xians.yaco.model.domain.Category;
import com.xians.yaco.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <pre>
 *     文章分类API
 * </pre>
 *
 * @author : XIANS
 */
@RestController
@RequestMapping(value = "/api/categories")
public class ApiCategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 获取所有分类
     *
     * <p>
     * result json:
     * <pre>
     * {
     *     "code": 200,
     *     "msg": "OK",
     *     "result": [
     *         {
     *             "cateId": "",
     *             "cateName": "",
     *             "cateUrl": "",
     *             "cateDesc": ""
     *         }
     *     ]
     * }
     *     </pre>
     * </p>
     *
     * @return JsonResult
     */
    @GetMapping
    public List<Category> categories() {
        return categoryService.listAll();
    }

    /**
     * 获取单个分类的信息
     *
     * <p>
     * result json:
     * <pre>
     * {
     *     "code": 200,
     *     "msg": "OK",
     *     "result": {
     *         "cateId": "",
     *         "cateName": "",
     *         "cateUrl": "",
     *         "cateDesc": ""
     *     }
     * }
     *     </pre>
     * </p>
     *
     * @param cateUrl 分类路径
     *
     * @return JsonResult
     */
    @GetMapping(value = "/{cateUrl}")
    public Category categories(@PathVariable("cateUrl") String cateUrl) {
        return categoryService.findByCateUrl(cateUrl);
    }
}
