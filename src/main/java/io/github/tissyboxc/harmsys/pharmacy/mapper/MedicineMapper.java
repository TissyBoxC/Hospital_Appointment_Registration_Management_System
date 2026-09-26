package io.github.tissyboxc.harmsys.pharmacy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.tissyboxc.harmsys.pharmacy.entity.Medicine;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** medicine 表数据访问。 */
public interface MedicineMapper extends BaseMapper<Medicine> {

  @Update("UPDATE medicine SET stock_quantity=stock_quantity+#{quantity} WHERE id=#{id}")
  int stockIn(@Param("id") long id, @Param("quantity") BigDecimal quantity);
}
