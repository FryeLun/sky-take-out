package com.sky.controller.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        String key = "dish:" + categoryId;
        //查询redis中是否有缓存数据
        String jsonStr = stringRedisTemplate.opsForValue().get(key);
        List<DishVO> list = null;
        if(jsonStr != null && !jsonStr.isEmpty()) {
            try {
                list = objectMapper.readValue(jsonStr,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, DishVO.class));
                return Result.success(list);
            } catch (JsonProcessingException e) {
                log.error("Redis缓存反序列化失败", e);
            }
        }

        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品

        list = dishService.listWithFlavor(dish);

        //将查询到的数据载入缓存
        try {
            String jsonCache = objectMapper.writeValueAsString(list); // 生成JSON格式字符串
            stringRedisTemplate.opsForValue().set(key, jsonCache);
        } catch (JsonProcessingException e) {
            log.error("Redis缓存序列化失败", e);
        }

        return Result.success(list);
    }

}
