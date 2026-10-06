package io.github.cdsap.kotlinprocess.fixtures

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy
import java.util.Collections
import javax.inject.Inject

/**
 * TestKit-only stand-in for the Develocity 4.x settings plugin, resolved under the real plugin
 * ID `com.gradle.develocity` through
 * `src/test/resources/META-INF/gradle-plugins/com.gradle.develocity.properties`.
 *
 * It mirrors what the real plugin (4.6.0) exposes to other plugins:
 * - a `develocity` settings extension whose public type is the real
 *   `com.gradle.develocity.agent.gradle.DevelocityConfiguration`;
 * - the same instance added as a `develocity` extension on the root project from a
 *   `gradle.rootProject {}` hook, so it is visible to `projectsLoaded` listeners and root build
 *   scripts regardless of settings plugin order;
 * - `project.pluginManager.hasPlugin("com.gradle.develocity")` stays false.
 *
 * The Develocity API jar is compileOnly in production and absent from the test compile
 * classpath, so the interfaces are loaded reflectively from the TestKit plugin classpath and
 * implemented with [Proxy].
 *
 * Output format, one line per call:
 * - `SCAN-VALUE <name>=<value>` for `buildScan.value(name, value)`
 * - `SCAN-TAG <tag>` for `buildScan.tag(tag)`
 * - `SCAN-LINK <name>=<url>` for `buildScan.link(name, url)`
 *
 * `buildScan.buildFinished` actions travel through the configuration cache as a field of the
 * [FakeBuildFinishedHandoff] task (which finalizes every task), are handed to
 * [FakeBuildFinishedService] at execution time, and run when that service closes at build end.
 */
abstract class FakeDevelocityPlugin
    @Inject
    constructor(
        private val objects: ObjectFactory,
    ) : Plugin<Settings> {
        override fun apply(settings: Settings) {
            println(APPLIED_MARKER)
            val finishedActions = mutableListOf<Any>()
            val develocity = FakeDevelocityProxies.configuration(objects, finishedActions)

            @Suppress("UNCHECKED_CAST")
            val apiType = FakeDevelocityProxies.loadApiType(FakeDevelocityProxies.DEVELOCITY_CONFIGURATION) as Class<Any>
            settings.extensions.add(apiType, "develocity", develocity)
            settings.gradle.rootProject {
                extensions.add(apiType, "develocity", develocity)
            }

            val service =
                settings.gradle.sharedServices.registerIfAbsent(
                    SERVICE_NAME,
                    FakeBuildFinishedService::class.java,
                ) {}

            settings.gradle.projectsEvaluated {
                val root = rootProject
                val handoff =
                    root.tasks.register(HANDOFF_TASK_NAME, FakeBuildFinishedHandoff::class.java) {
                        usesService(service)
                        this.service.set(service)
                        this.finishedActions = finishedActions
                    }
                allprojects {
                    tasks.configureEach {
                        if (!(project == root && name == HANDOFF_TASK_NAME)) finalizedBy(handoff)
                    }
                }
            }
        }

        companion object {
            const val APPLIED_MARKER = "FAKE-DEVELOCITY applied to settings"

            // Named to sort after the plugin's `kotlinProcessService`, matching the sibling fake.
            // Ordering is not load-bearing here: the plugin's buildFinished action reads the
            // jstat/jinfo value sources directly and shares no state with that service.
            private const val SERVICE_NAME = "zzzFakeDevelocityBuildFinished"
            private const val HANDOFF_TASK_NAME = "fakeDevelocityBuildFinished"
        }
    }

/**
 * Carries the recorded buildFinished actions through the configuration cache. Build service
 * parameters are isolated with Java serialization, which the plugin's action lambdas do not
 * support (they capture non-Serializable state), whereas task fields are bean-serialized.
 */
abstract class FakeBuildFinishedHandoff : DefaultTask() {
    @get:Internal
    abstract val service: Property<FakeBuildFinishedService>

    @get:Internal
    var finishedActions: List<Any> = emptyList()

    @TaskAction
    fun handOff() {
        service.get().finishedActions.addAll(finishedActions)
    }
}

abstract class FakeBuildFinishedService :
    BuildService<BuildServiceParameters.None>,
    AutoCloseable {
    val finishedActions: MutableList<Any> = Collections.synchronizedList(mutableListOf())

    override fun close() {
        val buildResult =
            FakeDevelocityProxies.create(FakeDevelocityProxies.BUILD_RESULT) { _, method, _ ->
                if (method.name == "getFailures") emptyList<Throwable>() else FakeDevelocityProxies.UNHANDLED
            }
        finishedActions.forEach {
            @Suppress("UNCHECKED_CAST")
            (it as Action<Any>).execute(buildResult)
        }
    }
}

object FakeDevelocityProxies {
    const val DEVELOCITY_CONFIGURATION = "com.gradle.develocity.agent.gradle.DevelocityConfiguration"
    const val BUILD_SCAN_CONFIGURATION = "com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration"
    const val BUILD_RESULT = "com.gradle.develocity.agent.gradle.scan.BuildResult"
    val UNHANDLED = Any()

    fun loadApiType(name: String): Class<*> = FakeDevelocityProxies::class.java.classLoader.loadClass(name)

    fun configuration(
        objects: ObjectFactory?,
        finishedActions: MutableList<Any>,
    ): Any {
        val buildScan = buildScan(objects, finishedActions)
        return create(DEVELOCITY_CONFIGURATION, objects) { _, method, args ->
            when (method.name) {
                "getBuildScan" -> buildScan
                "buildScan" -> execute(args[0], buildScan)
                else -> UNHANDLED
            }
        }
    }

    private fun buildScan(
        objects: ObjectFactory?,
        finishedActions: MutableList<Any>,
    ): Any =
        create(BUILD_SCAN_CONFIGURATION, objects) { proxy, method, args ->
            when (method.name) {
                "value" -> println("SCAN-VALUE ${args[0]}=${args[1]}")
                "tag" -> println("SCAN-TAG ${args[0]}")
                "link" -> println("SCAN-LINK ${args[0]}=${args[1]}")
                "buildFinished" -> finishedActions.add(args[0]!!)
                "background" -> execute(args[0], proxy)
                else -> return@create UNHANDLED
            }
            null
        }

    fun create(
        apiType: String,
        objects: ObjectFactory? = null,
        override: (proxy: Any, method: Method, args: Array<Any?>) -> Any?,
    ): Any {
        val type = loadApiType(apiType)
        return Proxy.newProxyInstance(type.classLoader, arrayOf(type), DefaultsHandler(type, objects, override))
    }

    private fun execute(
        action: Any?,
        target: Any,
    ): Any? {
        @Suppress("UNCHECKED_CAST")
        (action as Action<Any>).execute(target)
        return null
    }

    /** Answers anything `override` leaves [UNHANDLED] with a no-op or a sensible default. */
    private class DefaultsHandler(
        private val type: Class<*>,
        private val objects: ObjectFactory?,
        private val override: (Any, Method, Array<Any?>) -> Any?,
    ) : InvocationHandler {
        private val properties = mutableMapOf<String, Property<*>>()

        override fun invoke(
            proxy: Any,
            method: Method,
            args: Array<Any?>?,
        ): Any? {
            val arguments = args ?: emptyArray()
            if (method.declaringClass == Any::class.java) {
                return when (method.name) {
                    "equals" -> proxy === arguments[0]
                    "hashCode" -> System.identityHashCode(proxy)
                    else -> "Fake${type.simpleName}"
                }
            }
            val overridden = override(proxy, method, arguments)
            if (overridden !== UNHANDLED) return overridden
            if (method.returnType == Property::class.java && objects != null) {
                return synchronized(properties) {
                    properties.getOrPut(method.name) {
                        val valueType = (method.genericReturnType as ParameterizedType).actualTypeArguments[0] as Class<*>
                        objects.property(valueType)
                    }
                }
            }
            return when (method.returnType) {
                java.lang.Boolean.TYPE -> false
                Integer.TYPE -> 0
                java.lang.Long.TYPE -> 0L
                else -> null
            }
        }
    }
}
