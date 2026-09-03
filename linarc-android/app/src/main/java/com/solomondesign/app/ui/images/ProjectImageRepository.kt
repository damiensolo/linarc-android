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

    /**
     * Bundled site photos for images created at runtime without real pixels (the "Use demo
     * image" source and `addPhoto` fallbacks) — kept distinct from the seeded photos so a
     * demo-added image doesn't visually duplicate a grid neighbour. Credits: PHOTO_CREDITS.md.
     */
    private val demoPhotoPool = listOf(
        R.drawable.site_concrete_pour,
        R.drawable.site_rebar_inspection,
        R.drawable.site_crew_panels,
    )

    /** A real bundled photo for a runtime-added demo image; any [seed] picks deterministically. */
    fun demoPhotoSource(seed: Int): ImageSource =
        ImageSource.Drawable(demoPhotoPool[((seed % demoPhotoPool.size) + demoPhotoPool.size) % demoPhotoPool.size])

    /** Distinct tags across all images, sorted — drives the filter chips. */
    fun visibleTags(): List<String> = _images.flatMap { it.tags }.distinct().sorted()

    fun filterByTag(tag: String?): List<ProjectImage> =
        if (tag == null) _images.toList() else _images.filter { tag in it.tags }

    /** Distinct album names, sorted — drives the album picker and the Albums view. */
    fun albums(): List<String> =
        _images.mapNotNull { it.album?.takeIf(String::isNotBlank) }.distinct().sorted()

    /** Files (or, with null/blank, un-files) a photo into an album. Missing ids are no-ops. */
    fun setAlbum(id: String, album: String?) {
        val index = _images.indexOfFirst { it.id == id }
        if (index < 0) return
        _images[index] = _images[index].copy(album = album?.trim()?.takeIf(String::isNotEmpty))
    }

    fun add(image: ProjectImage) {
        _images.add(0, image)
    }

    /**
     * Swaps the pixels behind an image in place — the markup editor's "replace the original",
     * which is why the image is also flagged [ProjectImage.hasMarkup]. The id, title, Plan pin,
     * and Today row all survive because they key on the id; only the source changes. Callers
     * must pass a NEW file path rather than rewriting the old file ([FilePhoto] decode is keyed
     * on the path, so a rewrite would leave stale bitmaps on screen); the old capture file is
     * deleted here once the swap lands.
     */
    fun replaceSource(id: String, newSource: ImageSource) {
        val index = _images.indexOfFirst { it.id == id }
        if (index < 0) return
        val old = _images[index].source
        _images[index] = _images[index].copy(source = newSource, hasMarkup = true)
        if (old is ImageSource.CapturedFile && old != newSource) {
            CapturedMediaStore.delete(old.absolutePath)
        }
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
        (image.source as? ImageSource.CapturedFile)?.let { CapturedMediaStore.delete(it.absolutePath) }
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
                    source = ImageSource.Drawable(R.drawable.site_corridor_framing),
                    album = "Progress set",
                ),
                ProjectImage(
                    id = "img-col4-conflict",
                    title = "Column 4 med gas conflict",
                    area = "Area B · Column 4",
                    tags = listOf("Column 4", "Issue"),
                    capturedAtMillis = now - 3 * hour,
                    authorName = "Sam Reyes",
                    source = ImageSource.Drawable(R.drawable.site_medgas_overhead),
                    album = "Deficiencies",
                ),
                ProjectImage(
                    id = "img-conduit-exam6",
                    title = "Branch conduit rough-in, exam 6",
                    area = "Area B · Level 2",
                    tags = listOf("Area B", "Electrical"),
                    capturedAtMillis = now - 5 * hour,
                    authorName = "Maria Chen",
                    source = ImageSource.Drawable(R.drawable.site_conduit_roughin),
                ),
                ProjectImage(
                    id = "img-firecaulk",
                    title = "Fire-caulk at level 2 north",
                    area = "Area B · Level 2 north",
                    tags = listOf("Complete", "Firestopping"),
                    capturedAtMillis = now - 26 * hour,
                    authorName = "Dave Miller",
                    source = ImageSource.Drawable(R.drawable.site_firecaulk),
                ),
                ProjectImage(
                    id = "img-crew-hector",
                    title = "Hector Ortiz on site",
                    area = "Area B",
                    tags = listOf("Area B", "Crew"),
                    capturedAtMillis = now - 28 * hour,
                    authorName = CurrentUser.NAME,
                    source = ImageSource.Drawable(R.drawable.crew_hector),
                    album = "Crew",
                ),
                ProjectImage(
                    id = "img-crew-maria",
                    title = "Maria Chen — electrical rough-in",
                    area = "Area B · Level 2",
                    tags = listOf("Crew", "Electrical"),
                    capturedAtMillis = now - 30 * hour,
                    authorName = CurrentUser.NAME,
                    source = ImageSource.Drawable(R.drawable.crew_maria),
                    album = "Crew",
                ),
                ProjectImage(
                    id = "img-yesterday",
                    title = "Yesterday progress — Area B",
                    area = "Area B · structural framing",
                    tags = listOf("Area B", "Progress"),
                    capturedAtMillis = now - 24 * hour,
                    authorName = "Hector Ortiz",
                    source = ImageSource.Drawable(R.drawable.site_framing_progress),
                    album = "Progress set",
                ),
                ProjectImage(
                    id = "img-door-bucks",
                    title = "Doors and panels staged, level 2",
                    area = "Area B · Level 2",
                    tags = listOf("Area B", "Materials"),
                    capturedAtMillis = now - 32 * hour,
                    authorName = "Dave Miller",
                    source = ImageSource.Drawable(R.drawable.site_doors_staged),
                ),
            ),
        )
    }
}
