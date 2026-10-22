package com.dawahayah.app

import com.dawahayah.app.core.contentpacks.Anatomy3DAsset
import com.dawahayah.app.core.contentpacks.AssetPlayerState
import com.dawahayah.app.core.contentpacks.Stage01AnatomyAssetPlayer
import com.dawahayah.app.core.locale.ArabicSearchNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage 02 JVM Unit Tests for Anatomy, 3D Asset Player Contract, and Diseases Search Normalization.
 */
class Stage02AnatomyDiseasesUnitTest {

    @Test
    fun `ArabicSearchNormalizer normalizes Arabic disease and anatomical terms`() {
        val raw = "الأُذَيْنُ الأَيْمَنُ وَفَرْطُ ضَغْطِ الدَّمِ"
        val normalized = ArabicSearchNormalizer.normalize(raw)
        assertTrue(normalized.contains("اذين"))
        assertTrue(normalized.contains("فرط ضغط الدم"))
    }

    @Test
    fun `Stage01AnatomyAssetPlayer resolves bundled low-LOD Heart GLB to ReadyToRender3D`() {
        val player = Stage01AnatomyAssetPlayer()
        val heartLowAsset = Stage01AnatomyAssetPlayer.STAGE_01_REGISTERED_3D_CONTRACTS.first {
            it.assetId == "3d-cor-chambers-low-v1"
        }
        val state = player.resolveAssetState(
            asset = heartLowAsset,
            isPackInstalled = false,
            isWebGlSupported = true
        )
        assertTrue(state is AssetPlayerState.ReadyToRender3D)
        val ready = state as AssetPlayerState.ReadyToRender3D
        assertEquals("models/cor_chambers_low_v1.glb", ready.verifiedLocalGlbPath)
        assertEquals(6, ready.asset.selectableSubMeshes.size)
    }

    @Test
    fun `Stage01AnatomyAssetPlayer falls back to 2D diagram when WebGL is unsupported on device GPU`() {
        val player = Stage01AnatomyAssetPlayer()
        val heartLowAsset = Stage01AnatomyAssetPlayer.STAGE_01_REGISTERED_3D_CONTRACTS.first()
        val state = player.resolveAssetState(
            asset = heartLowAsset,
            isPackInstalled = true,
            isWebGlSupported = false
        )
        assertTrue(state is AssetPlayerState.FallbackTo2DDiagram)
    }

    @Test
    fun `Stage01AnatomyAssetPlayer requests pack download for high-LOD cloud atlas when not installed`() {
        val player = Stage01AnatomyAssetPlayer()
        val heartHighAsset = Stage01AnatomyAssetPlayer.STAGE_01_REGISTERED_3D_CONTRACTS.first {
            it.assetId == "3d-cor-chambers-high-v1"
        }
        val state = player.resolveAssetState(
            asset = heartHighAsset,
            isPackInstalled = false,
            isWebGlSupported = true
        )
        assertTrue(state is AssetPlayerState.RequiresPackDownload)
        val req = state as AssetPlayerState.RequiresPackDownload
        assertEquals("heart-model-high-v1", req.requiredPackId)
        assertEquals(Anatomy3DAsset.QualityLevel.HIGH_ATLAS, req.asset.qualityLevel)
    }
}
