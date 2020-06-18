package com.xians.yaco.web.controller.api;


import com.xians.yaco.model.domain.Gallery;
import com.xians.yaco.service.GalleryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <pre>
 *     图库API
 * </pre>
 *
 * @author : XIANS
 */
@RestController
@RequestMapping(value = "/api/galleries")
public class ApiGalleryController {

    @Autowired
    private GalleryService galleryService;

    /**
     * 获取所有图片
     *
     * <p>
     * result json:
     * <pre>
     * {
     *     "code": 200,
     *     "msg": "OK",
     *     "result": [
     *         {
     *             "galleryId": ,
     *             "galleryName": "",
     *             "galleryDesc": "",
     *             "galleryDate": "",
     *             "galleryLocation": "",
     *             "galleryThumbnailUrl": "",
     *             "galleryUrl": ""
     *         }
     *     ]
     * }
     *     </pre>
     * </p>
     *
     * @return JsonResult
     */
    @GetMapping
    public List<Gallery> galleries() {
        return galleryService.listAll();
    }

    /**
     * 获取单张图片的信息
     *
     * <p>
     * result json:
     * <pre>
     * {
     *     "code": 200,
     *     "msg": "OK",
     *     "result": [
     *         {
     *             "galleryId": ,
     *             "galleryName": "",
     *             "galleryDesc": "",
     *             "galleryDate": "",
     *             "galleryLocation": "",
     *             "galleryThumbnailUrl": "",
     *             "galleryUrl": ""
     *         }
     *     ]
     * }
     *     </pre>
     * </p>
     *
     * @param id id
     *
     * @return JsonResult
     */
    @GetMapping(value = "/{id}")
    public Gallery galleries(@PathVariable("id") Long id) {
        return galleryService.getById(id);
    }
}
