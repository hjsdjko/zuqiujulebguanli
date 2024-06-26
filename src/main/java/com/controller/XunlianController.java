
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 训练计划
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/xunlian")
public class XunlianController {
    private static final Logger logger = LoggerFactory.getLogger(XunlianController.class);

    private static final String TABLE_NAME = "xunlian";

    @Autowired
    private XunlianService xunlianService;


    @Autowired
    private TokenService tokenService;

    @Autowired
    private DictionaryService dictionaryService;//字典
    @Autowired
    private GonggaoService gonggaoService;//公告信息
    @Autowired
    private HetongService hetongService;//合同
    @Autowired
    private JiaolianService jiaolianService;//教练
    @Autowired
    private SaishiService saishiService;//赛事
    @Autowired
    private ShujuService shujuService;//球员数据
    @Autowired
    private YonghuService yonghuService;//用户
    @Autowired
    private UsersService usersService;//管理员


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("用户".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        else if("教练".equals(role))
            params.put("jiaolianId",request.getSession().getAttribute("userId"));
        params.put("xunlianDeleteStart",1);params.put("xunlianDeleteEnd",1);
        CommonUtil.checkMap(params);
        PageUtils page = xunlianService.queryPage(params);

        //字典表数据转换
        List<XunlianView> list =(List<XunlianView>)page.getList();
        for(XunlianView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        XunlianEntity xunlian = xunlianService.selectById(id);
        if(xunlian !=null){
            //entity转view
            XunlianView view = new XunlianView();
            BeanUtils.copyProperties( xunlian , view );//把实体数据重构到view中
            //级联表 用户
            //级联表
            YonghuEntity yonghu = yonghuService.selectById(xunlian.getYonghuId());
            if(yonghu != null){
            BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "username", "password", "newMoney", "yonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setYonghuId(yonghu.getId());
            }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody XunlianEntity xunlian, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,xunlian:{}",this.getClass().getName(),xunlian.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");
        else if("用户".equals(role))
            xunlian.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

        Wrapper<XunlianEntity> queryWrapper = new EntityWrapper<XunlianEntity>()
            .eq("yonghu_id", xunlian.getYonghuId())
            .eq("xunlian_name", xunlian.getXunlianName())
            .eq("xunlian_types", xunlian.getXunlianTypes())
            .eq("xunlian_kemu", xunlian.getXunlianKemu())
            .eq("xunlian_time", new SimpleDateFormat("yyyy-MM-dd").format(xunlian.getXunlianTime()))
            .eq("xunlian_delete", 1)
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        XunlianEntity xunlianEntity = xunlianService.selectOne(queryWrapper);
        if(xunlianEntity==null){
            xunlian.setXunlianDelete(1);
            xunlian.setInsertTime(new Date());
            xunlian.setCreateTime(new Date());
            xunlianService.insert(xunlian);
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody XunlianEntity xunlian, HttpServletRequest request) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        logger.debug("update方法:,,Controller:{},,xunlian:{}",this.getClass().getName(),xunlian.toString());
        XunlianEntity oldXunlianEntity = xunlianService.selectById(xunlian.getId());//查询原先数据

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
//        else if("用户".equals(role))
//            xunlian.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
        if("".equals(xunlian.getXunlianPhoto()) || "null".equals(xunlian.getXunlianPhoto())){
                xunlian.setXunlianPhoto(null);
        }
        if("".equals(xunlian.getXunlianContent()) || "null".equals(xunlian.getXunlianContent())){
                xunlian.setXunlianContent(null);
        }

            xunlianService.updateById(xunlian);//根据id更新
            return R.ok();
    }



    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids, HttpServletRequest request){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        List<XunlianEntity> oldXunlianList =xunlianService.selectBatchIds(Arrays.asList(ids));//要删除的数据
        ArrayList<XunlianEntity> list = new ArrayList<>();
        for(Integer id:ids){
            XunlianEntity xunlianEntity = new XunlianEntity();
            xunlianEntity.setId(id);
            xunlianEntity.setXunlianDelete(2);
            list.add(xunlianEntity);
        }
        if(list != null && list.size() >0){
            xunlianService.updateBatchById(list);
        }

        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //.eq("time", new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
        try {
            List<XunlianEntity> xunlianList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            XunlianEntity xunlianEntity = new XunlianEntity();
//                            xunlianEntity.setYonghuId(Integer.valueOf(data.get(0)));   //用户 要改的
//                            xunlianEntity.setXunlianName(data.get(0));                    //训练计划名称 要改的
//                            xunlianEntity.setXunlianUuidNumber(data.get(0));                    //训练计划编号 要改的
//                            xunlianEntity.setXunlianPhoto("");//详情和图片
//                            xunlianEntity.setXunlianTypes(Integer.valueOf(data.get(0)));   //训练计划类型 要改的
//                            xunlianEntity.setXunlianKemu(data.get(0));                    //训练科目 要改的
//                            xunlianEntity.setXunlianTime(sdf.parse(data.get(0)));          //日期 要改的
//                            xunlianEntity.setXunlianContent("");//详情和图片
//                            xunlianEntity.setXunlianDelete(1);//逻辑删除字段
//                            xunlianEntity.setInsertTime(date);//时间
//                            xunlianEntity.setCreateTime(date);//时间
                            xunlianList.add(xunlianEntity);


                            //把要查询是否重复的字段放入map中
                                //训练计划编号
                                if(seachFields.containsKey("xunlianUuidNumber")){
                                    List<String> xunlianUuidNumber = seachFields.get("xunlianUuidNumber");
                                    xunlianUuidNumber.add(data.get(0));//要改的
                                }else{
                                    List<String> xunlianUuidNumber = new ArrayList<>();
                                    xunlianUuidNumber.add(data.get(0));//要改的
                                    seachFields.put("xunlianUuidNumber",xunlianUuidNumber);
                                }
                        }

                        //查询是否重复
                         //训练计划编号
                        List<XunlianEntity> xunlianEntities_xunlianUuidNumber = xunlianService.selectList(new EntityWrapper<XunlianEntity>().in("xunlian_uuid_number", seachFields.get("xunlianUuidNumber")).eq("xunlian_delete", 1));
                        if(xunlianEntities_xunlianUuidNumber.size() >0 ){
                            ArrayList<String> repeatFields = new ArrayList<>();
                            for(XunlianEntity s:xunlianEntities_xunlianUuidNumber){
                                repeatFields.add(s.getXunlianUuidNumber());
                            }
                            return R.error(511,"数据库的该表中的 [训练计划编号] 字段已经存在 存在数据为:"+repeatFields.toString());
                        }
                        xunlianService.insertBatch(xunlianList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }




    /**
    * 前端列表
    */
    @IgnoreAuth
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("list方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));

        CommonUtil.checkMap(params);
        PageUtils page = xunlianService.queryPage(params);

        //字典表数据转换
        List<XunlianView> list =(List<XunlianView>)page.getList();
        for(XunlianView c:list)
            dictionaryService.dictionaryConvert(c, request); //修改对应字典表字段

        return R.ok().put("data", page);
    }

    /**
    * 前端详情
    */
    @RequestMapping("/detail/{id}")
    public R detail(@PathVariable("id") Integer id, HttpServletRequest request){
        logger.debug("detail方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        XunlianEntity xunlian = xunlianService.selectById(id);
            if(xunlian !=null){


                //entity转view
                XunlianView view = new XunlianView();
                BeanUtils.copyProperties( xunlian , view );//把实体数据重构到view中

                //级联表
                    YonghuEntity yonghu = yonghuService.selectById(xunlian.getYonghuId());
                if(yonghu != null){
                    BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "username", "password", "newMoney", "yonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setYonghuId(yonghu.getId());
                }
                //修改对应字典表字段
                dictionaryService.dictionaryConvert(view, request);
                return R.ok().put("data", view);
            }else {
                return R.error(511,"查不到数据");
            }
    }


    /**
    * 前端保存
    */
    @RequestMapping("/add")
    public R add(@RequestBody XunlianEntity xunlian, HttpServletRequest request){
        logger.debug("add方法:,,Controller:{},,xunlian:{}",this.getClass().getName(),xunlian.toString());
        Wrapper<XunlianEntity> queryWrapper = new EntityWrapper<XunlianEntity>()
            .eq("yonghu_id", xunlian.getYonghuId())
            .eq("xunlian_name", xunlian.getXunlianName())
            .eq("xunlian_uuid_number", xunlian.getXunlianUuidNumber())
            .eq("xunlian_types", xunlian.getXunlianTypes())
            .eq("xunlian_kemu", xunlian.getXunlianKemu())
            .eq("xunlian_delete", xunlian.getXunlianDelete())
//            .notIn("xunlian_types", new Integer[]{102})
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        XunlianEntity xunlianEntity = xunlianService.selectOne(queryWrapper);
        if(xunlianEntity==null){
            xunlian.setXunlianDelete(1);
            xunlian.setInsertTime(new Date());
            xunlian.setCreateTime(new Date());
        xunlianService.insert(xunlian);

            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

}

