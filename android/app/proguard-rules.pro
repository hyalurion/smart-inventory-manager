# ============================================================
# R8 全模式 ProGuard 规则（release）
# 目标：开启 minify + shrinkResources + R8 full mode 压缩包体积，
#      同时保留反射/序列化相关类，避免运行期崩溃。
# ============================================================

# 保留注解、签名等信息（Retrofit / Gson / kotlinx.serialization 运行期反射依赖）
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod, Exceptions, SourceFile, LineNumberTable

# ---- 数据 / 网络 / 本地存储层：包含 Retrofit 接口、Gson 模型、Room 实体、错误包络 ----
# 这些类被第三方库通过反射访问，必须整体保留成员，否则 release 会解析失败或序列化报错。
-keep class com.chronie.inventorymanager.data.** { *; }
-keep class com.chronie.inventorymanager.domain.** { *; }
# 采购清单模型在独立包 com.smartinventory.models 下（PurchaseListResponse / PurchaseListItem），
# 字段大多无 @SerializedName，Gson 靠字段名反射；R8 全模式会混淆字段名导致解析失败 -> 显示"无记录"。
-keep class com.smartinventory.** { *; }

# ---- kotlinx.serialization（Nav3 路由 @Serializable）官方推荐保留规则 ----
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class **$$serializer { *; }

# ---- Hilt（AGP 插件通常会自动注入规则，这里加保险） ----
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper

# ---- 保留序列化 / 可打包基类 ----
-keep class * implements android.os.Parcelable { *; }
-keep class * implements java.io.Serializable { *; }

# ---- 保留 Manifest 注册的主组件（保险） ----
-keep class com.chronie.inventorymanager.MainActivity { *; }

# 保留堆栈行号，便于在 release 崩溃日志里定位
-renamesourcefileattribute SourceFile
