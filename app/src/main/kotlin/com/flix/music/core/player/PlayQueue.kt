package com.flix.music.core.player

import com.flix.music.core.model.Track
import kotlin.random.Random

/**
 * The desktop queue, kept in play order: index `i` is the `i`-th song to play, which is
 * also what Cola shows, so every queue action addresses what the user sees. Shuffle
 * keeps the current song and mixes the rest; turning it off restores the original
 * order, keeping songs added meanwhile. Not thread-safe: the player uses it from one
 * thread.
 */
class PlayQueue(private val random: Random = Random.Default) {

    /** A queued song; [key] tells apart two copies of the same track. */
    private data class Entry(val key: Long, val track: Track)

    private var entries = mutableListOf<Entry>()
    private var unshuffled: List<Entry>? = null
    private var nextKey = 0L

    var currentIndex: Int = -1
        private set

    var repeatMode: RepeatMode = RepeatMode.Off

    val shuffleEnabled: Boolean get() = unshuffled != null

    val current: Track? get() = entries.getOrNull(currentIndex)?.track

    val size: Int get() = entries.size

    operator fun get(index: Int): Track = entries[index].track

    val tracks: List<Track> get() = entries.map(Entry::track)

    /** What plays after the current song, in order (repeat is not unrolled). */
    val upNext: List<QueueItem>
        get() = entries.indices.drop(currentIndex + 1).map { index -> QueueItem(index, entries[index].track) }

    val hasNext: Boolean get() = nextIndex(userSkip = true) != null

    val hasPrevious: Boolean get() = currentIndex > 0 || (repeatMode == RepeatMode.All && entries.isNotEmpty())

    /** Everything needed to rebuild this queue later, in play order. */
    data class Snapshot(
        val tracks: List<Track>,
        val currentIndex: Int,
        val repeatMode: RepeatMode,
        /** With shuffle on, the original order as indexes into [tracks]; null otherwise. */
        val unshuffledOrder: List<Int>?,
    )

    fun snapshot(): Snapshot? {
        if (entries.isEmpty()) return null
        val unshuffledOrder = unshuffled?.mapNotNull { kept -> entries.indexOfFirst { entry -> entry.key == kept.key }.takeIf { index -> index >= 0 } }
        return Snapshot(entries.map(Entry::track), currentIndex, repeatMode, unshuffledOrder)
    }

    fun restore(snapshot: Snapshot) {
        entries = snapshot.tracks.map(::newEntry).toMutableList()
        currentIndex = snapshot.currentIndex.coerceIn(entries.indices)
        repeatMode = snapshot.repeatMode
        unshuffled = snapshot.unshuffledOrder?.mapNotNull(entries::getOrNull)
    }

    fun replace(tracks: List<Track>, startIndex: Int, shuffle: Boolean) {
        entries = tracks.map(::newEntry).toMutableList()
        unshuffled = null
        currentIndex = startIndex.coerceIn(entries.indices)
        if (shuffle) setShuffle(true)
    }

    /**
     * The index that follows the current one: the next song, back to the first with
     * repeat all, or the same song when it simply ended with repeat one. Null at the end.
     */
    fun nextIndex(userSkip: Boolean): Int? = when {
        entries.isEmpty() -> null
        repeatMode == RepeatMode.One && !userSkip -> currentIndex
        currentIndex + 1 < entries.size -> currentIndex + 1
        repeatMode != RepeatMode.Off -> 0
        else -> null
    }

    fun previousIndex(): Int? = when {
        entries.isEmpty() -> null
        currentIndex > 0 -> currentIndex - 1
        repeatMode == RepeatMode.All -> entries.lastIndex
        else -> null
    }

    fun moveTo(index: Int) {
        if (index in entries.indices) currentIndex = index
    }

    fun playNext(track: Track) = insert(currentIndex + 1, track)

    fun add(track: Track) = insert(entries.size, track)

    fun addAll(tracks: List<Track>) = tracks.forEach(::add)

    /** Removes a song other than the current one. */
    fun remove(index: Int) {
        if (index !in entries.indices || index == currentIndex) return
        val removed = entries.removeAt(index)
        unshuffled = unshuffled?.filterNot { entry -> entry.key == removed.key }
        if (index < currentIndex) currentIndex--
    }

    fun restore(index: Int, track: Track) = insert(index.coerceIn(0, entries.size), track)

    fun move(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in entries.indices || toIndex !in entries.indices || fromIndex == toIndex) return
        val currentKey = entries[currentIndex].key
        entries.add(toIndex, entries.removeAt(fromIndex))
        currentIndex = entries.indexOfFirst { entry -> entry.key == currentKey }
    }

    /** "Limpiar": only the current song stays. */
    fun clearUpNext() {
        val currentEntry = entries.getOrNull(currentIndex) ?: return
        entries = mutableListOf(currentEntry)
        unshuffled = unshuffled?.filter { entry -> entry.key == currentEntry.key }
        currentIndex = 0
    }

    fun setShuffle(enabled: Boolean) {
        if (enabled == shuffleEnabled || entries.isEmpty()) return
        val currentEntry = entries[currentIndex]
        if (enabled) {
            unshuffled = entries.toList()
            entries = (listOf(currentEntry) + entries.filterNot { entry -> entry.key == currentEntry.key }.shuffled(random)).toMutableList()
            currentIndex = 0
        } else {
            val original = requireNotNull(unshuffled)
            val addedMeanwhile = entries.filterNot { entry -> original.any { kept -> kept.key == entry.key } }
            entries = (original + addedMeanwhile).toMutableList()
            currentIndex = entries.indexOfFirst { entry -> entry.key == currentEntry.key }
            unshuffled = null
        }
    }

    private fun insert(index: Int, track: Track) {
        val entry = newEntry(track)
        entries.add(index, entry)
        if (entries.size == 1) currentIndex = 0 else if (index <= currentIndex) currentIndex++
    }

    private fun newEntry(track: Track) = Entry(nextKey++, track)
}
