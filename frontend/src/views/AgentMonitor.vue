<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getMonitorStats, getFailureStats, getRecentLogs, type TokenStat, type FailureStat, type ExecutionLog } from '../api/monitor'
import { message } from 'ant-design-vue'
import { 
    DashboardOutlined, 
    ThunderboltOutlined, 
    BugOutlined, 
    HistoryOutlined,
    ReloadOutlined
} from '@ant-design/icons-vue'

const loading = ref(false)
const tokenStats = ref<TokenStat[]>([])
const failureStats = ref<FailureStat[]>([])
const recentLogs = ref<ExecutionLog[]>([])

// 计算总指标
const totalTokensConsumed = computed(() => {
    return tokenStats.value.reduce((sum, item) => sum + item.total_tokens, 0)
})

const totalSkillCalls = computed(() => {
    return tokenStats.value.reduce((sum, item) => sum + item.call_count, 0)
})

const totalFailures = computed(() => {
    return failureStats.value.reduce((sum, item) => sum + item.fail_count, 0)
})

const loadData = async () => {
    loading.value = true
    try {
        const [resToken, resFail, resLogs] = await Promise.all([
            getMonitorStats(),
            getFailureStats(),
            getRecentLogs()
        ])
        
        if (resToken.code === 200) tokenStats.value = resToken.data
        if (resFail.code === 200) failureStats.value = resFail.data
        if (resLogs.code === 200) recentLogs.value = resLogs.data
        
    } catch (e) {
        message.error('加载监控数据失败')
    } finally {
        loading.value = false
    }
}

onMounted(() => {
    loadData()
    // 自动刷新 (可选)
    // setInterval(loadData, 30000)
})

// 表格列定义
const skillColumns = [
    { title: '技能名称', dataIndex: 'executor_name', key: 'executor_name' },
    { title: '调用次数', dataIndex: 'call_count', key: 'call_count', sorter: (a: any, b: any) => a.call_count - b.call_count },
    { title: '总 Token 消耗', dataIndex: 'total_tokens', key: 'total_tokens', sorter: (a: any, b: any) => a.total_tokens - b.total_tokens },
    { title: '平均 Token/次', key: 'avg', customRender: ({ record }: any) => Math.round(record.total_tokens / record.call_count) }
]

const logColumns = [
    { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
    { title: 'Session ID', dataIndex: 'sessionId', key: 'sessionId', ellipsis: true },
    { title: '类型', dataIndex: 'actionType', key: 'actionType', width: 120 },
    { title: '执行器 / 模型', key: 'executor', customRender: ({ record }: any) => record.executorName === 'LLM_CORE' ? record.model : record.executorName },
    { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
    { title: '耗时', dataIndex: 'durationMs', key: 'durationMs', customRender: ({ text }: any) => `${text} ms` },
    { title: 'Token', dataIndex: 'totalTokens', key: 'totalTokens' }
]
</script>

<template>
    <div class="monitor-container">
        <div class="header">
            <h2>
                <DashboardOutlined /> Agent 实时监控台
            </h2>
            <a-button @click="loadData" :loading="loading">
                <template #icon><ReloadOutlined /></template>
                刷新数据
            </a-button>
        </div>

        <!-- 概览卡片 -->
        <div class="stats-cards">
            <a-card class="stat-card">
                <a-statistic title="总 Token 消耗" :value="totalTokensConsumed" groupSeparator=",">
                    <template #prefix><ThunderboltOutlined style="color: #faad14" /></template>
                </a-statistic>
            </a-card>
            
            <a-card class="stat-card">
                <a-statistic title="Skill 调用总数" :value="totalSkillCalls" groupSeparator=",">
                    <template #prefix><HistoryOutlined style="color: #1890ff" /></template>
                </a-statistic>
            </a-card>
            
            <a-card class="stat-card">
                <a-statistic title="异常次数" :value="totalFailures" :value-style="{ color: totalFailures > 0 ? '#cf1322' : '#3f8600' }">
                    <template #prefix><BugOutlined /></template>
                </a-statistic>
            </a-card>
        </div>

        <a-row :gutter="24" class="main-content">
            <!-- 左侧：技能排行榜 -->
            <a-col :span="10">
                <a-card title="技能消耗排行" :bordered="false" class="chart-card">
                    <a-table 
                        :dataSource="tokenStats" 
                        :columns="skillColumns" 
                        rowKey="executor_name" 
                        :pagination="false" 
                        size="small"
                    />
                </a-card>
            </a-col>

            <!-- 右侧：实时日志 -->
            <a-col :span="14">
                <a-card title="最近执行日志 (Live)" :bordered="false" class="chart-card">
                    <a-table 
                        :dataSource="recentLogs" 
                        :columns="logColumns" 
                        rowKey="id" 
                        :pagination="{ pageSize: 10 }" 
                        size="small"
                    >
                        <template #bodyCell="{ column, record }">
                            <template v-if="column.key === 'actionType'">
                                <a-tag :color="record.actionType === 'LLM_CHAT' ? 'blue' : 'purple'">
                                    {{ record.actionType === 'LLM_CHAT' ? 'LLM' : 'SKILL' }}
                                </a-tag>
                            </template>
                            <template v-if="column.key === 'status'">
                                <a-tag :color="record.status === 'SUCCESS' ? 'success' : 'error'">
                                    {{ record.status }}
                                </a-tag>
                            </template>
                            <template v-if="column.key === 'createdAt'">
                                {{ new Date(record.createdAt).toLocaleString() }}
                            </template>
                        </template>
                    </a-table>
                </a-card>
            </a-col>
        </a-row>
    </div>
</template>

<style scoped>
.monitor-container {
    /* Inherit from parent */
}

.header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
}

.header h2 {
    margin: 0;
    font-size: 24px;
    color: #1f1f1f;
}

.stats-cards {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 24px;
    margin-bottom: 24px;
}

.stat-card {
    border-radius: 8px;
    box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.03);
}

.chart-card {
    border-radius: 8px;
    min-height: 500px;
}
</style>
