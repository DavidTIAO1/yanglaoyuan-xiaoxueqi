<template>
  <div class="profile-page">
    <div class="profile-card">
      <div class="card-header">
        <el-icon :size="24"><UserFilled /></el-icon>
        <h3>个人信息</h3>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        class="profile-form"
      >
        <div class="readonly-section">
          <el-form-item label="用户名">
            <el-input :model-value="auth.user?.username" disabled />
          </el-form-item>
          <el-form-item label="家庭ID">
            <div class="family-id-display">
              <el-input :model-value="auth.user?.familyId" disabled />
              <el-button text type="primary" @click="copyFamilyId" :icon="CopyDocument" size="small">复制</el-button>
            </div>
          </el-form-item>
        </div>

        <el-divider />

        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" :prefix-icon="User" />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" :prefix-icon="Phone" />
        </el-form-item>

        <el-form-item label="配送地址" prop="address">
          <el-input
            v-model="form.address"
            type="textarea"
            :rows="3"
            placeholder="请输入详细配送地址（语音下单时将自动使用此地址）"
          />
          <div class="field-hint">设置后，语音聊天下单会自动使用此地址，无需每次手动输入</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave" class="save-btn">
            保存修改
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { UserFilled, User, Phone, CopyDocument } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { userApi } from '../../api'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const formRef = ref(null)
const saving = ref(false)

const form = ref({
  realName: '',
  phone: '',
  address: ''
})

const rules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
}

onMounted(() => {
  form.value.realName = auth.user?.realName || ''
  form.value.phone = auth.user?.phone || ''
  form.value.address = auth.user?.address || ''
})

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await userApi.updateProfile({
      realName: form.value.realName,
      phone: form.value.phone,
      address: form.value.address || undefined
    })
    // Refresh auth store with updated profile
    await auth.fetchProfile()
    ElMessage.success('保存成功')
  } catch (e) {
    // handled by interceptor
  } finally {
    saving.value = false
  }
}

function copyFamilyId() {
  if (!auth.user?.familyId) return
  navigator.clipboard.writeText(auth.user.familyId).then(() => {
    ElMessage.success('家庭ID已复制')
  }).catch(() => {
    ElMessage.info('家庭ID: ' + auth.user.familyId)
  })
}
</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px - 48px);
  overflow-y: auto;
}

.profile-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  padding: 32px 40px;
  flex: 1;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 28px;
  color: #4F46E5;
}

.card-header h3 {
  margin: 0;
  font-size: 1.2rem;
  color: #1F2937;
}

.readonly-section .el-form-item {
  margin-bottom: 16px;
}

.family-id-display {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.family-id-display .el-input {
  flex: 1;
}

.field-hint {
  font-size: 0.8rem;
  color: #9CA3AF;
  margin-top: 4px;
}

.save-btn {
  width: 160px;
  height: 42px;
  font-size: 0.95rem;
  border-radius: 10px;
  background: #4F46E5;
  border-color: #4F46E5;
}

.el-divider {
  margin: 20px 0;
}
</style>
