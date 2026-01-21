<script setup lang="ts">

// Assuming images will be placed here by the user
// If you want to use the generated images, ensure they are moved to this path
import normalImg from '@/assets/images/mascot_normal.png'
import hideImg from '@/assets/images/mascot_hide.png'

const props = defineProps<{
    mode: 'normal' | 'password'
}>()

</script>

<template>
    <div class="mascot-wrapper">
        <div class="mascot-container">
            <img :src="normalImg" alt="Mascot Normal" class="mascot-img" :class="{ active: mode === 'normal' }" />
            <img :src="hideImg" alt="Mascot Hide" class="mascot-img" :class="{ active: mode === 'password' }" />
        </div>
    </div>
</template>

<style scoped>
.mascot-wrapper {
    position: absolute;
    top: -130px;
    /* Adjust based on mascot size to sit on top of the card */
    left: 0;
    width: 100%;
    display: flex;
    justify-content: center;
    pointer-events: none;
    /* Let clicks pass through */
    z-index: 10;
}

.mascot-container {
    position: relative;
    width: 160px;
    /* Adjust based on image size */
    height: 120px;
}

.mascot-img {
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    height: auto;
    opacity: 0;
    transform: translateY(10px);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.mascot-img.active {
    opacity: 1;
    transform: translateY(0);
}
</style>
