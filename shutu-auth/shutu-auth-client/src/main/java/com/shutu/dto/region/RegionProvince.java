

package com.shutu.dto.region;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 省
 *
 * @author jiujingz@126.com
 */
@Schema(description = "省")
@Data
@EqualsAndHashCode(callSuper = true)
public class RegionProvince extends Region {
    @Schema(description = "市列表")
    private List<Region> cities = new ArrayList<>();
}
