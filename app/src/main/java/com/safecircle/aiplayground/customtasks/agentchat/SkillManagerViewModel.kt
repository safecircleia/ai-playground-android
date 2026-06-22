package com.safecircle.aiplayground.customtasks.agentchat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safecircle.aiplayground.common.LOCAL_URL_BASE
import com.safecircle.aiplayground.common.SkillTryOutChip
import com.safecircle.aiplayground.data.DataStoreRepository
import com.safecircle.aiplayground.proto.Skill
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStreamReader
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "AGSkillManagerVM"

enum class SkillSource(val sourceName: String) {
  BUILTIN("builtin"),
  FEATURED("featured"),
  REMOTE_URL("remote_url"),
  LOCAL_IMPORT("local_import"),
  UNKNOWN("unknown"),
}

data class SkillState(val skill: Skill)

data class SkillManagerUiState(
  val loading: Boolean = false,
  val skills: List<SkillState> = listOf(),
  val validating: Boolean = false,
  val validationError: String? = null,
  val importDirectoryUri: Uri? = null,
)

@HiltViewModel
class SkillManagerViewModel
@Inject
constructor(
  val dataStoreRepository: DataStoreRepository,
  @ApplicationContext private val context: Context,
) : ViewModel() {
  private val _uiState = MutableStateFlow(SkillManagerUiState())
  val uiState = _uiState.asStateFlow()
  var skillLoaded = false

  suspend fun loadSkills() {
    if (!skillLoaded) {
      setLoading(true)
      withContext(Dispatchers.IO) {
        val allDataStoreSkills = dataStoreRepository.getAllSkills()
        val dataStoreBuiltInSkills = allDataStoreSkills.filter { it.builtIn }
        val dataStoreCustomSkills = allDataStoreSkills.filter { !it.builtIn }

        val builtInSelectionMap = dataStoreBuiltInSkills.associate {
          it.name to Pair(it.selected, it.userModifiedSelection)
        }

        val builtInSkills = loadBuiltInSkills(context, builtInSelectionMap, DEFAULT_DISABLED_SKILLS)

        val finalSkills = builtInSkills.toMutableList()
        for (customSkill in dataStoreCustomSkills) {
          if (!finalSkills.any { it.name == customSkill.name }) {
            finalSkills.add(customSkill)
          }
        }

        dataStoreRepository.setSkills(finalSkills)

        _uiState.update { it.copy(skills = finalSkills.map { s -> SkillState(skill = s) }) }
        setLoading(false)
        skillLoaded = true
      }
    }
  }

  fun validateAndAddSkillFromUrl(url: String, onSuccess: () -> Unit, onValidationError: (String) -> Unit) {
    setValidating(true)
    setValidationError(null)
    viewModelScope.launch(Dispatchers.IO) {
      try {
        var normalizedUrl = url.removeSuffix("/SKILL.md").removeSuffix("/")
        val skillMdUrl = "$normalizedUrl/SKILL.md"
        val mdContent = try {
          InputStreamReader(URL(skillMdUrl).openConnection().getInputStream()).use { it.readText() }
        } catch (e: Exception) {
          val error = "Failed to fetch SKILL.md: ${e.message}"
          setValidationError(error); onValidationError(error); return@launch
        }
        if (mdContent.isEmpty()) {
          val error = "SKILL.md is empty"; setValidationError(error); onValidationError(error); return@launch
        }
        val (skillProto, errors) = convertSkillMdToProto(mdContent, builtIn = false, selected = true, skillUrl = normalizedUrl)
        if (errors.isNotEmpty()) {
          val error = errors.joinToString(", "); setValidationError(error); onValidationError(error); return@launch
        }
        skillProto?.let { skill ->
          if (_uiState.value.skills.any { it.skill.name == skill.name }) {
            val error = "A skill with name '${skill.name}' already exists."; setValidationError(error); onValidationError(error); return@launch
          }
          addSkill(skill = skill, addToDataStore = true)
          onSuccess()
        }
      } finally { setValidating(false) }
    }
  }

  fun validateAndAddSkillFromLocalImport(onSuccess: () -> Unit, onValidationError: (String) -> Unit) {
    setValidating(true)
    setValidationError(null)
    val directoryUri = _uiState.value.importDirectoryUri
    if (directoryUri == null) { setValidating(false); onValidationError("No directory URI set."); return }

    viewModelScope.launch(Dispatchers.IO) {
      try {
        val rootFile = DocumentFile.fromTreeUri(context, directoryUri)
        val skillMdFile = rootFile?.findFile("SKILL.md")
        if (skillMdFile == null || !skillMdFile.exists()) {
          val error = "SKILL.md not found"; setValidationError(error); onValidationError(error); return@launch
        }
        val mdContent = context.contentResolver.openInputStream(skillMdFile.uri)?.use { it.bufferedReader().readText() } ?: ""
        val (skillProto, errors) = convertSkillMdToProto(mdContent, builtIn = false, selected = true)
        if (errors.isNotEmpty()) {
          val error = errors.joinToString(", "); setValidationError(error); onValidationError(error); return@launch
        }
        skillProto?.let { proto ->
          if (_uiState.value.skills.any { it.skill.name == proto.name }) {
            val error = "A skill with name '${proto.name}' already exists."; setValidationError(error); onValidationError(error); return@launch
          }
          val originalImportDirName = getDisplayName(context, directoryUri)
          val destDir = getSkillDestinationDir(originalImportDirName)
          if (destDir.exists()) destDir.deleteRecursively()
          destDir.mkdirs()

          val sourceDocumentFile = DocumentFile.fromTreeUri(context, directoryUri)
          if (sourceDocumentFile != null) copyDocumentFile(sourceDocumentFile, destDir)

          val skillWithDir = proto.toBuilder().setImportDirName(destDir.relativeTo(context.filesDir).path).build()
          addSkill(skill = skillWithDir, addToDataStore = true)
          onSuccess()
        }
      } finally { setValidating(false); setImportDirectoryUri(null) }
    }
  }

  private fun copyDocumentFile(source: DocumentFile, dest: File) {
    if (source.isDirectory) {
      dest.mkdirs()
      for (child in source.listFiles()) { copyDocumentFile(child, File(dest, child.name!!)) }
    } else if (source.isFile) {
      runCatching {
        context.contentResolver.openInputStream(source.uri)?.use { input -> dest.outputStream().use { input.copyTo(it) } }
      }
    }
  }

  fun setLoading(loading: Boolean) { _uiState.update { it.copy(loading = loading) } }
  fun setValidating(validating: Boolean) { _uiState.update { it.copy(validating = validating) } }
  fun setValidationError(error: String?) { _uiState.update { it.copy(validationError = error) } }
  fun setImportDirectoryUri(uri: Uri?) { _uiState.update { it.copy(importDirectoryUri = uri) } }

  fun addSkill(skill: Skill, addToDataStore: Boolean) {
    _uiState.update { currentState ->
      val newSkillState = SkillState(skill = skill)
      if (skill.builtIn) currentState.copy(skills = currentState.skills + newSkillState)
      else {
        val firstCustomIndex = currentState.skills.indexOfFirst { !it.skill.builtIn }
        val newSkills = if (firstCustomIndex == -1) currentState.skills + newSkillState
        else currentState.skills.toMutableList().apply { add(firstCustomIndex, newSkillState) }
        currentState.copy(skills = newSkills)
      }
    }
    if (addToDataStore) { viewModelScope.launch(Dispatchers.IO) { dataStoreRepository.addSkill(skill) } }
  }

  fun deleteSkill(name: String) {
    val skill = _uiState.value.skills.firstOrNull { it.skill.name == name }?.skill ?: return
    _uiState.update { it.copy(skills = it.skills.filter { s -> s.skill.name != name }) }
    viewModelScope.launch(Dispatchers.IO) {
      if (skill.importDirName.isNotEmpty()) {
        runCatching { context.filesDir.resolve(skill.importDirName).deleteRecursively() }
      }
      dataStoreRepository.deleteSkill(name)
    }
  }

  fun deleteSkills(names: Set<String>) {
    val skillsToDelete = _uiState.value.skills.filter { names.contains(it.skill.name) }.map { it.skill }
    if (skillsToDelete.isEmpty()) return
    _uiState.update { it.copy(skills = it.skills.filter { s -> !names.contains(s.skill.name) }) }
    viewModelScope.launch(Dispatchers.IO) {
      skillsToDelete.forEach { skill ->
        if (skill.importDirName.isNotEmpty()) runCatching { context.filesDir.resolve(skill.importDirName).deleteRecursively() }
      }
      dataStoreRepository.deleteSkills(names)
    }
  }

  fun setSkillSelected(skill: SkillState, selected: Boolean) {
    val updatedSkill = skill.skill.toBuilder().setSelected(selected).build()
    _uiState.update { currentState ->
      currentState.copy(skills = currentState.skills.map {
        if (it.skill.name == skill.skill.name) SkillState(skill = updatedSkill) else it
      })
    }
    viewModelScope.launch(Dispatchers.IO) { dataStoreRepository.setSkillSelected(skill.skill, selected) }
  }

  fun setAllSkillsSelected(selected: Boolean) {
    _uiState.update { it.copy(skills = it.skills.map { s -> SkillState(skill = s.skill.toBuilder().setSelected(selected).build()) }) }
    viewModelScope.launch(Dispatchers.IO) { dataStoreRepository.setAllSkillsSelected(selected) }
  }

  fun getSelectedSkills(): List<Skill> = _uiState.value.skills.filter { it.skill.selected }.map { it.skill }
  fun getSkill(name: String): Skill? = _uiState.value.skills.firstOrNull { it.skill.name == name }?.skill

  fun getJsSkillUrl(skillName: String, scriptName: String): String? {
    val skill = getSkill(name = skillName) ?: return null
    val baseUrl = when {
      skill.importDirName.isNotEmpty() -> "$LOCAL_URL_BASE/${skill.importDirName}"
      skill.skillUrl.isNotEmpty() -> skill.skillUrl
      else -> return null
    }
    return "$baseUrl/scripts/$scriptName"
  }

  fun getJsSkillWebviewUrl(skillName: String, url: String): String {
    if (url.startsWith("http")) return url
    val skill = getSkill(name = skillName) ?: return url
    val baseUrl = when {
      skill.importDirName.isNotEmpty() -> "$LOCAL_URL_BASE/${skill.importDirName}"
      skill.skillUrl.isNotEmpty() -> skill.skillUrl
      else -> return url
    }
    return "$baseUrl/assets/$url"
  }

  fun getSelectedSkillsNamesAndDescriptions(): String =
    getSelectedSkills().joinToString("\n") { "- ${it.name}: ${it.description}" }

  fun isSkillSelected(skillName: String): Boolean =
    _uiState.value.skills.firstOrNull { it.skill.name == skillName }?.skill?.selected == true

  fun saveSkillEdit(
    index: Int, name: String, description: String, instructions: String,
    scriptsContent: Map<String, String>, onSuccess: () -> Unit, onError: (String) -> Unit,
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val isNewSkill = index < 0 || index >= _uiState.value.skills.size
        val normalizedName = name.replace("\\s+".toRegex(), "-")
        if (isNewSkill) {
          if (_uiState.value.skills.any { it.skill.name == normalizedName }) { onError("Name already exists."); return@launch }
          val skillDestDir = context.filesDir.resolve("skills/$normalizedName")
          if (skillDestDir.exists()) skillDestDir.deleteRecursively()
          skillDestDir.mkdirs()
          val scriptDestDir = File(skillDestDir, "scripts").also { it.mkdirs() }
          writeSkillMd(File(skillDestDir, "SKILL.md"), normalizedName, description, instructions)
          saveScripts(scriptDestDir, scriptsContent)
          val newSkill = Skill.newBuilder().setName(normalizedName).setDescription(description)
            .setInstructions(instructions).setBuiltIn(false).setSelected(true)
            .setImportDirName(skillDestDir.relativeTo(context.filesDir).path).build()
          addSkill(newSkill, addToDataStore = true)
          onSuccess()
        } else {
          val existingSkill = _uiState.value.skills[index].skill
          if (existingSkill.builtIn) { onError("Cannot edit built-in skills."); return@launch }
          val oldName = existingSkill.name
          val newSkillDestDir = context.filesDir.resolve("skills/$normalizedName")
          var updatedImportDirName = existingSkill.importDirName
          if (oldName != normalizedName) {
            if (_uiState.value.skills.any { it.skill.name == normalizedName }) { onError("Name already exists."); return@launch }
            val oldDir = context.filesDir.resolve(existingSkill.importDirName)
            if (oldDir.exists()) { oldDir.renameTo(newSkillDestDir); updatedImportDirName = newSkillDestDir.relativeTo(context.filesDir).path }
            else { newSkillDestDir.mkdirs() }
          }
          writeSkillMd(File(newSkillDestDir, "SKILL.md"), normalizedName, description, instructions)
          val scriptDir = File(newSkillDestDir, "scripts")
          scriptDir.deleteRecursively(); scriptDir.mkdirs()
          saveScripts(scriptDir, scriptsContent)
          val updatedSkill = existingSkill.toBuilder().setName(normalizedName).setDescription(description)
            .setInstructions(instructions).setImportDirName(updatedImportDirName).build()
          _uiState.update { it.copy(skills = it.skills.mapIndexed { i, s -> if (i == index) SkillState(skill = updatedSkill) else s }) }
          updateSkillInDataStore(oldName, updatedSkill)
          onSuccess()
        }
      } catch (e: Exception) { onError("Failed to save skill: ${e.message}") }
    }
  }

  fun loadSkillScriptsContent(skill: Skill, onDone: (Map<String, String>) -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
      if (skill.importDirName.isEmpty()) { withContext(Dispatchers.Main) { onDone(emptyMap()) }; return@launch }
      val scriptDir = File(context.filesDir.resolve(skill.importDirName), "scripts")
      if (!scriptDir.exists()) { withContext(Dispatchers.Main) { onDone(emptyMap()) }; return@launch }
      val scripts = mutableMapOf<String, String>()
      scriptDir.listFiles()?.filter { it.isFile && (it.name.endsWith(".html") || it.name.endsWith(".js")) }
        ?.forEach { scripts[it.name] = runCatching { it.readText() }.getOrDefault("") }
      withContext(Dispatchers.Main) { onDone(scripts) }
    }
  }

  fun deleteSkillScript(skill: Skill, scriptName: String) {
    if (skill.importDirName.isEmpty()) return
    viewModelScope.launch(Dispatchers.IO) {
      File(context.filesDir.resolve(skill.importDirName), "scripts/$scriptName").delete()
    }
  }

  fun getSkillShortId(skill: Skill): String {
    val source = getSkillSource(skill)
    val identifier = when (source) {
      SkillSource.BUILTIN, SkillSource.FEATURED -> skill.name
      SkillSource.LOCAL_IMPORT -> skill.importDirName
      else -> skill.skillUrl
    }
    if (identifier.isEmpty()) return "xxxx"
    val prefix = when (source) {
      SkillSource.BUILTIN -> "b_"; SkillSource.FEATURED -> "f_"; SkillSource.LOCAL_IMPORT -> "l_"; else -> "c_"
    }
    return try {
      val digest = java.security.MessageDigest.getInstance("SHA-256")
      prefix + digest.digest(identifier.toByteArray()).joinToString("") { "%02x".format(it) }.take(4)
    } catch (_: Exception) { prefix + "fail" }
  }

  private fun getSkillSource(skill: Skill): SkillSource = when {
    skill.builtIn -> SkillSource.BUILTIN
    skill.skillUrl.isNotEmpty() -> SkillSource.REMOTE_URL
    skill.importDirName.isNotEmpty() -> SkillSource.LOCAL_IMPORT
    else -> SkillSource.UNKNOWN
  }

  private fun writeSkillMd(file: File, name: String, description: String, instructions: String) {
    file.parentFile?.mkdirs()
    file.writeText("---\nname: $name\ndescription: $description\n---\n\n$instructions")
  }

  private fun saveScripts(dir: File, scripts: Map<String, String>) {
    dir.mkdirs()
    scripts.forEach { (name, content) -> File(dir, name).writeText(content) }
  }

  private fun updateSkillInDataStore(oldName: String, updatedSkill: Skill) {
    val allSkills = dataStoreRepository.getAllSkills()
    dataStoreRepository.setSkills(allSkills.map { if (it.name == oldName) updatedSkill else it })
  }

  private fun getSkillDestinationDir(originalImportDirName: String): File {
    return context.filesDir.resolve("skills/${originalImportDirName.replace("\\s+".toRegex(), "-")}")
  }

  companion object {
    val DEFAULT_DISABLED_SKILLS = setOf("calculate-hash", "kitchen-adventure", "text-spinner", "send-email")

    fun convertSkillMdToProto(
      mdContent: String, builtIn: Boolean, selected: Boolean, skillUrl: String = "", importDir: String = "",
    ): Pair<Skill?, List<String>> {
      val parts = mdContent.split("---")
      val errors = mutableListOf<String>()
      if (parts.size < 3) { errors.add("Invalid format"); return Pair(null, errors) }

      val header = parts[1].trim()
      var name: String? = null; var description: String? = null
      var requireSecret = false; var requireSecretDescription = ""; var homepage = ""
      var startMetadata = false
      for (line in header.lines()) {
        val t = line.trim()
        if (t == "metadata:") { startMetadata = true; continue }
        if (!startMetadata) {
          when { t.startsWith("name:") -> name = t.substringAfter("name:").trim()
            t.startsWith("description:") -> description = t.substringAfter("description:").trim() }
        } else {
          when { t.startsWith("require-secret:") -> requireSecret = t.substringAfter("require-secret:").trim().toBoolean()
            t.startsWith("require-secret-description:") -> requireSecretDescription = t.substringAfter("require-secret-description:").trim()
            t.startsWith("homepage:") -> homepage = t.substringAfter("homepage:").trim() }
        }
      }
      if (name.isNullOrEmpty()) errors.add("Missing 'name'")
      if (description.isNullOrEmpty()) errors.add("Missing 'description'")
      if (errors.isNotEmpty()) return Pair(null, errors)

      val instructions = parts.drop(2).joinToString("---").trim()
      val skill = Skill.newBuilder().setName(name!!).setDescription(description!!).setInstructions(instructions)
        .setBuiltIn(builtIn).setSelected(selected).setSkillUrl(skillUrl)
        .setRequireSecret(requireSecret).setRequireSecretDescription(requireSecretDescription)
        .setHomepage(homepage).setImportDirName(importDir).build()
      return Pair(skill, emptyList())
    }

    fun loadBuiltInSkills(
      context: Context,
      builtInSelectionMap: Map<String, Pair<Boolean, Boolean>> = emptyMap(),
      defaultDisabledSkills: Set<String> = emptySet(),
    ): List<Skill> {
      val builtInSkills = mutableListOf<Skill>()
      try {
        val skillAssetDirs = context.assets.list("skills") ?: emptyArray()
        for (dirName in skillAssetDirs) {
          try {
            context.assets.open("skills/$dirName/SKILL.md").use { inputStream ->
              val mdContent = inputStream.bufferedReader().readText()
              val (skillProto, errors) = convertSkillMdToProto(mdContent, builtIn = true, selected = true, importDir = "assets/skills/$dirName")
              if (errors.isEmpty() && skillProto != null) {
                val defaultSelected = skillProto.name !in defaultDisabledSkills
                val (persistedSelected, userModified) = builtInSelectionMap[skillProto.name] ?: Pair(defaultSelected, false)
                val selectedState = if (userModified) persistedSelected else defaultSelected
                builtInSkills.add(skillProto.toBuilder().setSelected(selectedState).setUserModifiedSelection(userModified).build())
              }
            }
          } catch (_: Exception) {}
        }
      } catch (e: Exception) { Log.e(TAG, "Error listing assets/skills", e) }
      return builtInSkills
    }
  }
}

fun getDisplayName(context: Context, uri: Uri): String {
  var name = ""
  runCatching {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
      val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (nameIndex != -1 && cursor.moveToFirst()) name = cursor.getString(nameIndex)
    }
  }
  return name.ifEmpty { uri.path?.substringAfterLast('/') ?: "Unknown" }
}

fun decodeBase64ToBitmap(base64String: String): Bitmap? {
  return try {
    val pureBase64 = base64String.substringAfter(",")
    val imageBytes = kotlin.io.encoding.Base64.decode(pureBase64)
    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
  } catch (_: Exception) { null }
}
