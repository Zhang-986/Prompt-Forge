<script setup lang="ts">
import { ref } from 'vue'
import { getUserHistory, type ArenaVoteHistoryItem } from '../../api/arena'
import { message } from 'ant-design-vue'
import { TrophyOutlined, CalendarOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'

const open = ref(false)
const history = ref<ArenaVoteHistoryItem[]>([])
const loading = ref(false)
const page = ref(1)
const size = 20
const hasMore = ref(true)

const show = () => {
  open.value = true
  // Reset and load
  page.value = 1
  history.value = []
  hasMore.value = true
  loadHistory()
}

const loadHistory = async () => {
  if (loading.value) return
  loading.value = true
  try {
    const res = await getUserHistory({ page: page.value, size })
    if (res.code === 200) {
      if (page.value === 1) {
        history.value = res.data.records
      } else {
        history.value.push(...res.data.records)
      }
      
      if (history.value.length >= res.data.total) {
        hasMore.value = false
      } else {
        page.value++
      }
    }
  } catch (e) {
    message.error('Failed to load history')
  } finally {
    loading.value = false
  }
}

const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

const emit = defineEmits(['restore'])

const onHistoryItemClick = (item: ArenaVoteHistoryItem) => {
  if (item.sessionId) {
    emit('restore', item)
  } else {
    // Legacy support or error handling
    message.warning('该记录为旧版数据，不支持回溯 (Legacy data not supported)')
  }
}

const hide = () => {
  open.value = false
}

defineExpose({ show, hide })
</script>

<template>
  <a-drawer
    v-model:open="open"
    title="Competition History"
    placement="right"
    width="480"
    :headerStyle="{ borderBottom: '1px solid #f3f4f6' }"
    :bodyStyle="{ padding: '0', background: '#f9fafb' }"
  >
    <div class="history-list">
      <div v-if="history.length === 0 && !loading" class="empty-state">
        No voting history yet.
      </div>
      
      <div v-for="item in history" :key="item.id" class="history-card" @click="onHistoryItemClick(item)">
        <div class="card-header">
          <div class="header-left">
            <span class="prompt-preview" :title="item.prompt">{{ item.prompt }}</span>
          </div>
          <div class="time-badge">
            <CalendarOutlined style="margin-right: 4px" />
            {{ formatDate(item.createdAt) }}
          </div>
        </div>
        
        <div class="battle-result">
          <div class="model-row winner">
            <div class="status-icon"><TrophyOutlined /></div>
            <div class="model-name">{{ item.winnerModel }}</div>
            <div class="result-tag win">WIN</div>
          </div>
          <div class="model-row loser">
            <div class="status-icon"></div>
            <div class="model-name">{{ item.loserModel }}</div>
            <div class="result-tag loss">LOSS</div>
          </div>
        </div>
      </div>

      <div v-if="hasMore" class="load-more">
        <a-button @click="loadHistory" :loading="loading" type="text" block>
          Load More
        </a-button>
      </div>
    </div>
  </a-drawer>
</template>

<style scoped>
.history-list {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.history-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  padding: 16px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.02);
  transition: all 0.2s;
  cursor: pointer;
}

.history-card:hover {
  border-color: #d1d5db;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  border-bottom: 1px solid #f3f4f6;
  padding-bottom: 12px;
}

.prompt-preview {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  max-width: 200px;
}

.time-badge {
  font-size: 12px;
  color: #9ca3af;
  display: flex;
  align-items: center;
}

.battle-result {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.model-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 6px;
  font-size: 13px;
}

.model-row.winner {
  background: #f0fdf4;
  color: #166534;
}

.model-row.loser {
  background: #f9fafb;
  color: #6b7280;
}

.status-icon {
  width: 16px;
  display: flex;
  justify-content: center;
}

.model-name {
  flex: 1;
  font-weight: 500;
}

.result-tag {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  letter-spacing: 0.5px;
}

.result-tag.win {
  background: #dcfce7;
  color: #15803d;
}

.result-tag.loss {
  background: #f3f4f6;
  color: #9ca3af;
}

.load-more {
  margin-top: 8px;
}

.empty-state {
  text-align: center;
  color: #9ca3af;
  padding: 40px;
  font-size: 14px;
}
</style>
