package com.xians.yaco.web.controller.api;


import com.xians.yaco.exception.NotFoundException;
import com.xians.yaco.model.domain.Tag;
import com.xians.yaco.model.dto.JsonResult;
import com.xians.yaco.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * <pre>
 *     标签API
 * </pre>
 *
 * @author : XIANS
 */
@CrossOrigin
@RequestMapping(value = "/api/tags")
public class ApiTagController {

    @Autowired
    private TagService tagService;

    /**
     * 获取所有标签
     *
     * <p>
     * result json:
     * <pre>
     * {
     *     "code": 200,
     *     "msg": "OK",
     *     "result": [
     *         {
     *             "tagId": ,
     *             "tagName": "",
     *             "tagUrl": ""
     *         }
     *     ]
     * }
     *     </pre>
     * </p>
     *
     * @return JsonResult
     */
    @GetMapping
    public JsonResult tags() {
        final List<Tag> tags = tagService.listAll();
        if (null != tags && tags.size() > 0) {
            return new JsonResult(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(), tags);
        } else {
            return new JsonResult(HttpStatus.NO_CONTENT.value(), HttpStatus.NO_CONTENT.getReasonPhrase());
        }
    }

    /**
     * 获取单个标签的信息
     *
     * <p>
     * result json:
     * <pre>
     * {
     *     "code": 200,
     *     "msg": "OK",
     *     "result": {
     *         "tagId": ,
     *         "tagName": "",
     *         "tagUrl": ""
     *     }
     * }
     *     </pre>
     * </p>
     *
     * @param tagUrl tagUrl
     *
     * @return JsonResult
     */
    @GetMapping(value = "/{tagUrl}")
    public Tag tags(@PathVariable("tagUrl") String tagUrl) {
        final Tag tag = tagService.findByTagUrl(tagUrl);

        if (tag == null) {
            throw new NotFoundException("Tag with url: " + tagUrl + " was not found");
        }

        return tag;
    }
}
