package com.zzk.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzk.infrastructure.persistence.po.ArenaVotePO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 竞技场投票 Mapper
 * 
 * @author zzk
 * @since 1.0.0
 */
@Mapper
public interface ArenaVoteMapper extends BaseMapper<ArenaVotePO> {
    
    /**
     * 统计模型获胜次数
     */
    @Select("SELECT winner_model as modelId, COUNT(*) as count FROM arena_votes GROUP BY winner_model")
    List<ModelWinCount> countWinsByModel();
    
    /**
     * 统计模型失败次数
     */
    @Select("SELECT loser_model as modelId, COUNT(*) as count FROM arena_votes GROUP BY loser_model")
    List<ModelWinCount> countLossesByModel();
    
    /**
     * 模型胜负统计结果
     */
    /**
     * 模型胜负统计结果
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class ModelWinCount {
        private String modelId;
        private Long count;
    }
}
