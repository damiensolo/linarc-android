package com.solomondesign.app.ui.images

import androidx.compose.runtime.mutableStateListOf
import com.solomondesign.app.R
import com.solomondesign.app.ui.collab.CurrentUser
import com.solomondesign.app.ui.demo.DemoProjectRepository

/** In-memory demo store for project images. Snapshot state, no ViewModel. */
object ProjectImageRepository {
    private val _images = mutableStateListOf<ProjectImage>()
    val images: List<ProjectImage> get() = _images

    init {
        seed()
    }

    fun find(id: String): ProjectImage? = _images.firstOrNull { it.id == id }

    /** Distinct tags across all images, sorted — drives the filter chips. */
    fun visibleTags(): List<String> = _images.flatMap { it.tags }.distinct().sorted()

    fun filterByTag(tag: String?): List<ProjectImage> =
        if (tag == null) _images.toList() else _images.filter { tag in it.tags }

    fun add(image: ProjectImage) {
        _images.add(0, image)
    }

    fun linkRecord(id: String, recordId: String) {
        val index = _images.indexOfFirst { it.id == id }
        if (index >= 0) _images[index] = _images[index].copy(linkedRecordId = recordId)
    }

    /**
     * Removes the image and everything it produced: the Plan pin and the Today stream entry
     * created alongside it, plus any captured bitmap. This cross-store cleanup is the most
     * regression-prone behaviour here, so it has a dedicated unit test.
     */
    fun delete(id: String) {
        val image = find(id) ?: return
        _images.removeAll { it.id == id }
        DemoProjectRepository.pins.removeAll { it.id == "pin-$id" }
        DemoProjectRepository.streamItems.removeAll { it.id == "stream-$id" }
        (image.source as? ImageSource.Captured)?.let { CapturedBitmapStore.remove(it.captureKey) }
    }

    fun clear() {
        _images.clear()
        CapturedBitmapStore.clear()
        seed()
    }

    private fun seed() {
        val now = System.currentTimeMillis()
        val hour = 3_600_000L
        _images.addAll(
            listOf(
                ProjectImage(
                    id = "img-corridor-c",
                    title = "Corridor C framing — Level 2",
                    area = "Area B · Level 2",
                    tags = listOf("Area B", "Framing", "Progress"),
                    capturedAtMillis = now - 2 * hour,
                    authorName = "Hector Ortiz",
                    source = ImageSource.Swatch(seed = 0),
                ),
                ProjectImage(
                    id = "img-col4-conflict",
                    title = "Column 4 med gas conflict",
                    area = "Area B · Column 4",
                    tags = listOf("Column 4", "Issue"),
                    capturedAtMillis = now - 3 * hour,
                    authorName = "Sam Reyes",
                    source = ImageSource.Swatch(seed = 1),
                ),
                ProjectImage(
                    id = "img-conduit-exam6",
                    title = "Branch conduit rough-in, exam 6",
                    area = "Area B · Level 2",
                    tags = listOf("Area B", "Electrical"),
                    capturedAtMillis = now - 5 * hour,
                    authorName = "Maria Chen",
                    source = ImageSource.Swatch(seed = 2),
                ),
                ProjectImage(
                    id = "img-firecaulk",
                    title = "Fire-caulk at level 2 north",
                    area = "Area B · Level 2 north",
                    tags = listOf("Complete", "Firestopping"),
                    capturedAtMillis = now - 26 * hour,
                    authorName = "Dave Miller",
                    source = ImageSource.Swatch(seed = 3),
                ),
                ProjectImage(
                    id = "img-crew-hector",
                    title = "Hector Ortiz on site",
                    area = "Area B",
                    tags = listOf("Area B", "Crew"),
                    capturedAtMillis = now - 28 * hour,
                    authorName = CurrentUser.NAME,
                    source = ImageSource.Drawable(R.drawable.crew_hector),
                ),
                ProjectImage(
                    id = "img-crew-maria",
                    title = "Maria Chen — electrical rough-in",
                    area = "Area B · Level 2",
                    tags = listOf("Crew", "Electrical"),
                    capturedAtMillis = now - 30 * hour,
                    authorName = CurrentUser.NAME,
                    source = ImageSource.Drawable(R.drawable.crew_maria),
                ),
                ProjectImage(
                    id = "img-yesterday",
                    title = "Yesterday progress — Area B",
                    area = "Area B · structural framing",
                    tags = listOf("Area B", "Progress"),
                    capturedAtMillis = now - 24 * hour,
                    authorName = "Hector Ortiz",
                    source = ImageSource.Swatch(seed = 4),
                ),
                ProjectImage(
                    id = "img-door-bucks",
                    title = "Door bucks staged, level 2",
                    area = "Area B · Level 2",
                    tags = listOf("Area B", "Materials"),
                    capturedAtMillis = now - 32 * hour,
                    authorName = "Dave Miller",
                    source = ImageSource.Swatch(seed = 5),
                ),
            ),
        )
    }
}
