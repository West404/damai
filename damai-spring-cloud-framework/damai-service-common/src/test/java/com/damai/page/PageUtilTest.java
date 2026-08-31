package com.damai.page;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.damai.dto.BasePageDto;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 分页工具 {@link PageUtil} 单元测试
 */
class PageUtilTest {

    @Test
    @DisplayName("getPageParams(BasePageDto)：按 dto 中的页码和页大小构建分页对象")
    void getPageParamsFromDto() {
        BasePageDto dto = new BasePageDto();
        dto.setPageNumber(2);
        dto.setPageSize(20);

        IPage<Object> page = PageUtil.getPageParams(dto);

        assertEquals(2L, page.getCurrent());
        assertEquals(20L, page.getSize());
    }

    @Test
    @DisplayName("getPageParams(int,int)：按页码和页大小构建分页对象")
    void getPageParamsFromNumbers() {
        IPage<Object> page = PageUtil.getPageParams(3, 10);

        assertEquals(3L, page.getCurrent());
        assertEquals(10L, page.getSize());
    }

    @Test
    @DisplayName("convertPage(PageInfo)：保留分页信息并对列表元素做类型转换")
    void convertPageFromPageInfo() {
        PageInfo<Integer> pageInfo = new PageInfo<>();
        pageInfo.setPageNum(1);
        pageInfo.setPageSize(3);
        pageInfo.setTotal(10L);
        pageInfo.setList(Arrays.asList(1, 2, 3));

        PageVo<String> pageVo = PageUtil.convertPage(pageInfo, i -> "num-" + i);

        assertEquals(1L, pageVo.getPageNum());
        assertEquals(3L, pageVo.getPageSize());
        assertEquals(10L, pageVo.getTotalSize());
        assertEquals(Arrays.asList("num-1", "num-2", "num-3"), pageVo.getList());
    }

    @Test
    @DisplayName("convertPage(IPage)：保留分页信息并对列表元素做类型转换")
    void convertPageFromIPage() {
        Page<Integer> page = new Page<>(2, 2);
        page.setTotal(5L);
        page.setRecords(Arrays.asList(3, 4));

        PageVo<String> pageVo = PageUtil.convertPage(page, String::valueOf);

        assertEquals(2L, pageVo.getPageNum());
        assertEquals(2L, pageVo.getPageSize());
        assertEquals(5L, pageVo.getTotalSize());
        assertEquals(List.of("3", "4"), pageVo.getList());
    }
}
