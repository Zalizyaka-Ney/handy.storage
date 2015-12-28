-keepattributes *Annotation*, AnnotationDefault

-keep class * extends handy.storage.api.Model { 
!private static final java.lang.String *; #you can comment this line if you don't use enforceColumnNameConstants option
@handy.storage.annotation.Column <fields>;
<init>(...); 
}

-keepclassmembers enum * {
	public static <fields>;
	public static **[] values();
    public static ** valueOf(java.lang.String);
}

-optimizations !class/unboxing/enum #this line can be commented if you disable the database schema changes checking

# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# Gson specific classes
-keep class sun.misc.Unsafe { *; }