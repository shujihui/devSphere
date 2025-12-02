

package com.shutu.feign.fallback;

import com.shutu.commons.tools.utils.Result;
//import com.shutu.feign.DictFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 字典接口 FallbackFactory
 *
 * @author jiujingz@126.com
 */
//@Slf4j
//@Component
//public class DictFeignClientFallbackFactory implements FallbackFactory<DictFeignClient> {
//    @Override
//    public DictFeignClient create(Throwable throwable) {
//        log.error("{}", throwable);
//        return () -> new Result<>();
//    }
//}
