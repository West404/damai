package com.damai.mybatisplus;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.damai.data.BaseTableData;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * MyBatis-Plus 自动填充处理器 {@link MybatisPlusMetaObjectHandler} 单元测试
 * <p>
 * strictFill 依赖已注册的 TableInfo，测试中用带主键的实体子类注册元数据
 */
class MybatisPlusMetaObjectHandlerTest {

    private final MybatisPlusMetaObjectHandler handler = new MybatisPlusMetaObjectHandler();

    /**
     * 测试专用实体：BaseTableData 不是实体（无主键），需要一个带 @TableId 的子类来注册 TableInfo
     */
    @TableName("t_meta_test")
    static class MetaTestEntity extends BaseTableData {
        @TableId
        private Long id;
    }

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), MetaTestEntity.class);
    }

    @Test
    @DisplayName("insertFill：插入时自动填充 createTime 和 editTime")
    void insertFillFillsCreateTimeAndEditTime() {
        MetaTestEntity data = new MetaTestEntity();
        MetaObject metaObject = SystemMetaObject.forObject(data);

        handler.insertFill(metaObject);

        assertNotNull(data.getCreateTime());
        assertNotNull(data.getEditTime());
    }

    @Test
    @DisplayName("updateFill：更新时只填充 editTime，不填充 createTime")
    void updateFillFillsOnlyEditTime() {
        MetaTestEntity data = new MetaTestEntity();
        MetaObject metaObject = SystemMetaObject.forObject(data);

        handler.updateFill(metaObject);

        assertNotNull(data.getEditTime());
        assertNull(data.getCreateTime());
    }
}
