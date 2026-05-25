package cn.yangeit.vo.admin;



import cn.yangeit.pojo.Elder;
import cn.yangeit.pojo.FamilyMember;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class FamilyMemberAdminVo extends FamilyMember {
    private List<Elder> elderList;
}
