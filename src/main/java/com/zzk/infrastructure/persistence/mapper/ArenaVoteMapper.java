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
     * 分页查询用户投票历史
     */
    @Select("SELECT v.id, v.session_id as sessionId, v.winner_model as winnerModel, v.loser_model as loserModel, v.created_at as createdAt, s.final_prompt as prompt "
            +
            "FROM arena_votes v " +
            "LEFT JOIN arena_sessions s ON v.session_id = s.id " +
            "WHERE v.voter_id = #{userId} " +
            "ORDER BY v.created_at DESC")
    List<com.zzk.interfaces.dto.response.ArenaVoteDTO> selectVotesByUserId(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.zzk.interfaces.dto.response.ArenaVoteDTO> page,
            @Param("userId") Long userId);

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
