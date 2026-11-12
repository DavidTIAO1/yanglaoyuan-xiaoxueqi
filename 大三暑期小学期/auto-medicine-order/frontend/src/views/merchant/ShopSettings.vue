<template>
  <div class="shop-settings-page">
    <div class="settings-card">
      <div v-if="tipMessage" class="tip-banner">
        <el-icon :size="18" color="#059669"><InfoFilled /></el-icon>
        <span>{{ tipMessage }}</span>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="settings-form"
      >
        <div class="form-grid">
          <el-form-item label="店铺名称" prop="shopName">
            <el-input v-model="form.shopName" placeholder="请输入店铺名称" />
          </el-form-item>

          <el-form-item label="营业执照号" prop="licenseNo">
            <el-input v-model="form.licenseNo" placeholder="请输入营业执照号" />
          </el-form-item>

          <el-form-item label="店铺地址" prop="address" class="full-width">
            <el-input v-model="form.address" placeholder="请输入店铺地址" />
          </el-form-item>

          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入联系电话" />
          </el-form-item>

          <el-form-item label="配送范围(km)" prop="deliveryRadius">
            <el-input-number
              v-model="form.deliveryRadius"
              :min="1"
              :max="50"
              :step="1"
              controls-position="right"
              style="width: 100%;"
            />
          </el-form-item>

          <el-form-item label="纬度" prop="latitude">
            <el-input-number
              v-model="form.latitude"
              :precision="6"
              :step="0.000001"
              controls-position="right"
              style="width: 100%;"
            />
          </el-form-item>

          <el-form-item label="经度" prop="longitude">
            <el-input-number
              v-model="form.longitude"
              :precision="6"
              :step="0.000001"
              controls-position="right"
              style="width: 100%;"
            />
          </el-form-item>
        </div>

        <div class="form-actions">
          <el-button
            type="success"
            size="large"
            round
            :loading="saving"
            @click="handleSubmit"
          >
            保存设置
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { merchantApi } from '../../api'

const formRef = ref(null)
const saving = ref(false)
const tipMessage = ref('')

const form = reactive({
  shopName: '',
  licenseNo: '',
  address: '',
  phone: '',
  latitude: undefined,
  longitude: undefined,
  deliveryRadius: 5
})

const rules = {
  shopName: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入店铺地址', trigger: 'blur' }]
}

onMounted(async () => {
  try {
    const res = await merchantApi.getProfile()
    const profile = res.data
    if (profile) {
      form.shopName = profile.shopName || ''
      form.licenseNo = profile.licenseNo || ''
      form.address = profile.address || ''
      form.phone = profile.phone || ''
      form.latitude = profile.latitude ?? undefined
      form.longitude = profile.longitude ?? undefined
      form.deliveryRadius = profile.deliveryRadius ?? 5
    }
  } catch (err) {
    const status = err?.response?.status
    if (status === 404) {
      tipMessage.value = '请先填写店铺信息以完成注册'
    }
  }
})

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  saving.value = true
  try {
    await merchantApi.updateProfile({
      shopName: form.shopName,
      licenseNo: form.licenseNo,
      address: form.address,
      phone: form.phone,
      latitude: form.latitude,
      longitude: form.longitude,
      deliveryRadius: form.deliveryRadius
    })
    tipMessage.value = ''
    ElMessage.success('设置已保存')
  } catch {
    // error interceptor handles the toast
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.shop-settings-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px - 48px);
}

.settings-card {
  background: white;
  border-radius: 14px;
  padding: 28px 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  flex: 1;
  overflow-y: auto;
}

.tip-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #ECFDF5;
  border-radius: 10px;
  margin-bottom: 24px;
  color: #065F46;
  font-size: 0.9rem;
}

.settings-form {
  max-width: 100%;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 24px;
}

.form-grid .full-width {
  grid-column: 1 / -1;
}

.form-actions {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}
</style>
