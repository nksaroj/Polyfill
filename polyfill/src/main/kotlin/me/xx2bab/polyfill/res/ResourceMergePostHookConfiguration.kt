package me.xx2bab.polyfill.res

import com.android.build.api.variant.ApplicationVariant
import com.android.build.gradle.internal.scope.InternalArtifactType
import me.xx2bab.polyfill.PolyfillAction
import me.xx2bab.polyfill.getApkCreationConfigImpl
import me.xx2bab.polyfill.getTaskContainer
import me.xx2bab.polyfill.task.SingleArtifactTaskExtendConfiguration
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

/**
 * Configurations for fetching required data and set up dependencies
 * through both explicit/implicit approaches.
 */
class ResourceMergePostHookConfiguration(
    project: Project,
    private val appVariant: ApplicationVariant,
    actionList: () -> List<PolyfillAction<Directory>>
) : SingleArtifactTaskExtendConfiguration<Directory>(project, appVariant, actionList) {

    override val data: Provider<Directory>
        get() = CreationAction(appVariant).extractMergedRes()

    override fun orchestrate() {
        // We try to avoid using afterEvaluate{},
        // but here it looks like the best workaround...
        project.afterEvaluate {
            // MergeResources
            val mergeTaskProvider = appVariant.getTaskContainer().mergeResourcesTask

            val localData = data

            // To consume the task instance here is ok,
            // since the merge task must run in a clean build,
            // it's not an avoidance task actually...
            val mergeTask = mergeTaskProvider.get()

            actionList().forEachIndexed { index, action ->
                action.onTaskConfigure(mergeTask)
                mergeTask.doLast("ResourceMergePostHookByPolyfill$index") {
                    action.onExecute(localData)
                }
            }
        }

        // If the getTaskContainer() does not work anymore,
        // we can fall back to below solution instead.
        // However, we should be aware of that
        // the `whenTaskAdded` is executed after `afterEavluate`.
//        val variantCapitalizedName = variant.getCapitalizedName()
//        project.tasks.whenTaskAdded {
//            if (name == "merge${variantCapitalizedName}Resources") {
//                val localData = data
//                actionList().forEachIndexed { index, action ->
//                    action.onTaskConfigure(this)
//                    doLast("ResourceMergePostHookByPolyfill$index") {
//                        action.onExecute(localData)
//                    }
//                }
//            }
//        }
    }

    class CreationAction(private val appVariant: ApplicationVariant) {
        fun extractMergedRes(): Provider<Directory> {
            return appVariant.getApkCreationConfigImpl()
                .config
                .artifacts
                .get(InternalArtifactType.MERGED_RES)
        }
    }

}