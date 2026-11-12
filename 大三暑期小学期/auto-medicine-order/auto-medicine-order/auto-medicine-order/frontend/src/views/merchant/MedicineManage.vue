<template>
  <div class="medicine-page">
    <!-- Toolbar -->
    <div class="toolbar">
      <el-button type="success" @click="openAddDialog">
        <el-icon style="margin-right: 4px;"><Plus /></el-icon>
        添加药品
      </el-button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="28"><Loading /></el-icon>
    </div>

    <!-- Empty state -->
    <div v-else-if="medicines.length === 0" class="empty-state" @click="openAddDialog">
      <el-icon :size="56" color="#A7F3D0"><Plus /></el-icon>
      <p>暂无药品，点击添加</p>
    </div>

    <!-- Medicine table -->
    <div v-else class="table-wrapper">
      <el-table :data="medicines" style="width: 100%" height="100%" stripe>
        <el-table-column prop="name" label="药品名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="genericName" label="通用名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" min-width="100" />
        <el-table-column label="价格" min-width="100">
          <template #default="{ row }">
            <span class="price-cell">¥{{ row.price.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" min-width="80" />
        <el-table-column label="处方药" min-width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.requiresPrescription ? 'warning' : 'info'" effect="plain" size="small">
              {{ row.requiresPrescription ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">下架</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Add/Edit dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑药品' : '添加药品'"
      width="500px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="80px"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="药品名称" prop="name">
              <el-input v-model="formData.name" placeholder="请输入药品名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="通用名" prop="genericName">
              <el-input v-model="formData.genericName" placeholder="请输入通用名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-input v-model="formData.category" placeholder="请输入分类" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="处方药" prop="requiresPrescription">
              <el-switch
                v-model="formData.requiresPrescription"
                active-text="是"
                inactive-text="否"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="价格" prop="price">
              <el-input-number
                v-model="formData.price"
                :min="0"
                :precision="2"
                :step="1"
                style="width: 100%"
                placeholder="请输入价格"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库存" prop="stock">
              <el-input-number
                v-model="formData.stock"
                :min="0"
                :step="1"
                style="width: 100%"
                placeholder="请输入库存"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入药品描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="success" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus, Loading } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { medicineApi } from '../../api'

const loading = ref(false)
const medicines = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const formData = reactive({
  name: '',
  genericName: '',
  category: '',
  price: 0,
  stock: 0,
  description: '',
  requiresPrescription: false
})

const formRules = {
  name: [{ required: true, message: '请输入药品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
}

onMounted(() => loadMedicines())

async function loadMedicines() {
  loading.value = true
  try {
    const res = await medicineApi.list()
    medicines.value = res.data || []
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function openAddDialog() {
  editingId.value = null
  Object.assign(formData, {
    name: '',
    genericName: '',
    category: '',
    price: 0,
    stock: 0,
    description: '',
    requiresPrescription: false
  })
  dialogVisible.value = true
}

function openEditDialog(row) {
  editingId.value = row.id
  Object.assign(formData, {
    name: row.name,
    genericName: row.genericName || '',
    category: row.category || '',
    price: row.price,
    stock: row.stock,
    description: row.description || '',
    requiresPrescription: !!row.requiresPrescription
  })
  dialogVisible.value = true
}

function resetForm() {
  editingId.value = null
  Object.assign(formData, {
    name: '',
    genericName: '',
    category: '',
    price: 0,
    stock: 0,
    description: '',
    requiresPrescription: false
  })
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = { ...formData }
    if (editingId.value) {
      const res = await medicineApi.update(editingId.value, payload)
      const updated = res.data
      const idx = medicines.value.findIndex(m => m.id === editingId.value)
      if (idx !== -1) medicines.value[idx] = updated
      ElMessage.success('已更新')
    } else {
      const res = await medicineApi.create(payload)
      medicines.value.push(res.data)
      ElMessage.success('已添加')
    }
    dialogVisible.value = false
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确认下架该药品？', '下架确认', {
      confirmButtonText: '确认下架',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await medicineApi.delete(row.id)
    medicines.value = medicines.value.filter(m => m.id !== row.id)
    ElMessage.success('已下架')
  } catch {
    // cancelled or error
  }
}
</script>

<style scoped>
.medicine-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px - 48px);
}

/* ── Toolbar ── */
.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

/* ── Loading / Empty ── */
.loading-state,
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #9CA3AF;
}

.empty-state {
  cursor: pointer;
  transition: color 0.2s;
}

.empty-state:hover {
  color: #059669;
}

.empty-state p {
  margin-top: 12px;
  font-size: 0.95rem;
}

/* ── Table ── */
.table-wrapper {
  flex: 1;
  min-height: 0;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #F3F4F6;
}

.price-cell {
  font-weight: 500;
  color: #374151;
}
</style>
