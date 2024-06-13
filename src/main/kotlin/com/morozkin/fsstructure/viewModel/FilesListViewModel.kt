package com.morozkin.fsstructure.viewModel

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.util.containers.isEmpty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.jewel.foundation.lazy.tree.Tree
import org.jetbrains.jewel.foundation.lazy.tree.TreeGeneratorScope
import org.jetbrains.jewel.foundation.lazy.tree.buildTree
import org.jetbrains.jewel.foundation.lazy.tree.emptyTree
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.*

@Immutable
sealed class FileSystemElement {
  abstract val name: String
  abstract val path: Path
  abstract val isReadable: Boolean
  abstract val isSymLink: Boolean
  abstract val displayAttributes: Boolean
  abstract val attributes: String?

  data class Directory(
    override val path: Path,
    override val isReadable: Boolean,
    override val isSymLink: Boolean,
    val isEmpty: Boolean,
    override val displayAttributes: Boolean = false,
    override val attributes: String? = null
  ) : FileSystemElement() {
    override val name get() = path.name.takeIf { it.isNotEmpty() } ?: path.pathString
  }

  data class File(
    override val path: Path,
    override val isReadable: Boolean,
    override val isSymLink: Boolean,
    override val displayAttributes: Boolean = false,
    override val attributes: String? = null
  ) : FileSystemElement() {
    override val name get() = path.name
  }
}

@Stable
@Service(Service.Level.PROJECT)
class FilesListViewModel(
  private val coroutineScope: CoroutineScope
) : SelectedFilesInfoBarViewModel {
  private lateinit var tree: FileSystemTree
  private val treeFlow = MutableSharedFlow<FileSystemTree>()

  val state: StateFlow<Tree<FileSystemElement>> = treeFlow
    .map { tree ->
      buildTree {
        tree.roots.forEach { process(it) }
      }
    }
    .stateIn(coroutineScope, SharingStarted.Eagerly, initialValue = emptyTree())

  private val _selectedFilesInfoBarState = MutableStateFlow<SelectedFilesInfoBarState>(SelectedFilesInfoBarState.File(emptyList()))
  override val selectedFilesInfoBarState: StateFlow<SelectedFilesInfoBarState> = _selectedFilesInfoBarState.asStateFlow()

  private fun TreeGeneratorScope<FileSystemElement>.process(element: FileSystemTree.Element) {
    when (element) {
      is FileSystemTree.Element.Node -> {
        addNode(
          id = element.element.path.pathString,
          data = element.element,
          childrenGenerator = element.children.let { children ->
            {
              children.forEach { process(it) }
            }
          }
        )
      }
      is FileSystemTree.Element.Leaf -> {
        addLeaf(
          id = element.element.path.pathString,
          data = element.element
        )
      }
    }
  }

  init {
    coroutineScope.launch(Dispatchers.IO) {
      val fs = FileSystems.getDefault()

      val roots = mutableListOf<FileSystemTree.Element>()
      fs.rootDirectories.forEach { rootDirPath ->
        try {
          val isReadable = rootDirPath.isReadable()
          val isSymbolicLink = rootDirPath.isSymbolicLink()
          val isEmpty = if (isReadable && !isSymbolicLink) rootDirPath.isEmptyDirectory else true

          val element = FileSystemElement.Directory(
            path = rootDirPath,
            isReadable = isReadable,
            isSymLink = isSymbolicLink,
            isEmpty = isEmpty,
            attributes = if (isReadable) rootDirPath.attributesString else null
          )

          if (isEmpty) {
            roots.add(
              FileSystemTree.Element.Leaf(element = element, parent = null)
            )
          } else {
            roots.add(
              FileSystemTree.Element.Node(element = element, parent = null)
            )
          }
        } catch (e: Exception) {
          thisLogger().error("Cannot determine root directories", e)
        }
      }

      tree = FileSystemTree(roots)
      treeFlow.emit(tree)
    }
  }

  fun handleSelectedElements(elements: List<FileSystemElement>) {
    if (elements.isEmpty()) {
      _selectedFilesInfoBarState.value = SelectedFilesInfoBarState.File(emptyList())
    } else if (elements.count() == 1) {
      _selectedFilesInfoBarState.value = SelectedFilesInfoBarState.File(elements.first().path.pathComponents)
    } else {
      // TODO: display size info as well
      _selectedFilesInfoBarState.value = SelectedFilesInfoBarState.Files(
        "${elements.count()} elements selected"
      )
    }
  }

  fun handleOpenedElements(elementsPathStings: List<String>) {
    coroutineScope.launch(Dispatchers.IO) {
      updateTree { tree ->
        val elementsToOpen = elementsPathStings
          .mapNotNull {
            tree.elementByPath(Path(it))?.element
          }
          .filter {
            !tree.hasChildren(it.path)
          }
        if (elementsToOpen.isEmpty()) return@updateTree

        elementsToOpen.forEach { element ->
          Files.newDirectoryStream(element.path).use { stream ->
            stream.forEach { path ->
              try {
                if (path.isRegularFile()) {
                  val isReadable = path.isReadable()
                  val attributes = if (isReadable) path.attributesString else null
                  tree.addLeaf(
                    parentPath = element.path,
                    element = FileSystemElement.File(path = path, isReadable = path.isReadable(), isSymLink = path.isSymbolicLink(), attributes = attributes)
                  )
                } else if (path.isDirectory()) {
                  val isSymLink = path.isSymbolicLink()
                  val isReadable = path.isReadable()
                  if (isReadable && !isSymLink) {
                    tree.addNode(
                      parentPath = element.path,
                      element = FileSystemElement.Directory(
                        path = path,
                        isReadable = true,
                        isSymLink = false,
                        isEmpty = path.isEmptyDirectory,
                        attributes = path.attributesString
                      )
                    )
                  } else {
                    tree.addLeaf(
                      parentPath = element.path,
                      element = FileSystemElement.Directory(
                        path = path,
                        isReadable = isReadable,
                        isSymLink = isSymLink,
                        isEmpty = true,
                        attributes = if (isReadable) path.attributesString else null
                      )
                    )
                  }
                }
              } catch (e: Exception) {
                thisLogger().error("Cannot fetch info about element at $path", e)
              }
            }
          }
        }
      }
    }
  }

  fun toggleAttributesVisibility(element: FileSystemElement) {
    coroutineScope.launch {
      updateTree {
        when (element) {
          is FileSystemElement.File -> {
            tree.updateLeafElement(element.path) { file ->
              if (file !is FileSystemElement.File) return@updateLeafElement file
              file.copy(displayAttributes = !file.displayAttributes)
            }
          }

          is FileSystemElement.Directory -> {
            if (element.isReadable && !element.isSymLink) {
              tree.updateNodeElement(element.path) { dir ->
                if (dir !is FileSystemElement.Directory) return@updateNodeElement dir
                dir.copy(displayAttributes = !dir.displayAttributes)
              }
            } else {
              tree.updateLeafElement(element.path) { dir ->
                if (dir !is FileSystemElement.Directory) return@updateLeafElement dir
                dir.copy(displayAttributes = !dir.displayAttributes)
              }
            }
          }
        }
      }
    }
  }

  // Events queue (stored in `atomic`) can be a good alternative to mutex, but will be a little bit complex
  private val mutex = Mutex()
  private suspend fun updateTree(action: (FileSystemTree) -> Unit) = mutex.withLock {
    action(tree)
    treeFlow.emit(tree)
  }

  private val Path.isEmptyDirectory: Boolean get() {
    if (!Files.isDirectory(this)) return false
    return Files.list(this).use { it.isEmpty() }
  }

  private val Path.pathComponents: List<String> get() {
    if (nameCount == 0) return listOf(pathString)
    val components = mutableListOf(root.pathString)
    for (i in 0 until nameCount) {
      components.add(
        getName(i).name
      )
    }
    return components.toList()
  }

  private val Path.attributesString: String?
    get() = Files.readAttributes(this, BasicFileAttributes::class.java)?.formattedString

  private val BasicFileAttributes.formattedString: String
    get() = "Created: ${creationTime()} | Modified: ${lastModifiedTime()} | Size: ${size().formattedSizeString}"

  private val Long.formattedSizeString: String
    get() = when {
      this == Long.MIN_VALUE || this < 0 -> "N/A"
      this < 1024L -> "$this B"
      this <= 0xfffccccccccccccL shr 40 -> "%.1f kB".format(this.toDouble() / (0x1 shl 10))
      this <= 0xfffccccccccccccL shr 30 -> "%.1f MB".format(this.toDouble() / (0x1 shl 20))
      this <= 0xfffccccccccccccL shr 20 -> "%.1f GB".format(this.toDouble() / (0x1 shl 30))
      this <= 0xfffccccccccccccL shr 10 -> "%.1f TB".format(this.toDouble() / (0x1 shl 40))
      this <= 0xfffccccccccccccL -> "%.1f PB".format((this shr 10).toDouble() / (0x1 shl 40))
      else -> "%.1f EB".format((this shr 20).toDouble() / (0x1 shl 40))
    }
}

private class FileSystemTree(
  val roots: List<Element>
) {
  sealed class Element {
    abstract var element: FileSystemElement
      internal set

    data class Leaf(override var element: FileSystemElement, val parent: Node?) : Element()

    data class Node(
      override var element: FileSystemElement,
      val parent: Node?
    ) : Element() {
      var children: List<Element> = emptyList()
        internal set
    }
  }

  private val parentNodes = roots
    .filterIsInstance<Element.Node>()
    .associateBy { it.element.path }
    .toMutableMap()

  fun hasChildren(parentPath: Path) = parentNodes[parentPath]?.children?.isNotEmpty() ?: false

  fun elementByPath(path: Path): Element? {
    val node = parentNodes[path]
    if (node != null) return node
    return parentNodes[path.parent]
  }

  fun addNode(parentPath: Path, element: FileSystemElement) {
    val parent = parentNodes[parentPath] ?: return

    val node = Element.Node(
      element = element,
      parent = parent
    )

    parent.children += node

    parentNodes[element.path] = node
  }

  fun updateNodeElement(path: Path, update: (FileSystemElement) -> FileSystemElement) {
    val node = parentNodes[path] ?: return
    node.element = update(node.element)

  }

  fun addLeaf(parentPath: Path, element: FileSystemElement) {
    val parent = parentNodes[parentPath] ?: return
    parent.children += Element.Leaf(element = element, parent = parent)
  }

  fun updateLeafElement(path: Path, update: (FileSystemElement) -> FileSystemElement) {
    val parentNode = parentNodes[path.parent] ?: return
    val node = parentNode.children.firstOrNull { it.element.path == path } ?: return
    node.element = update(node.element)
  }
}