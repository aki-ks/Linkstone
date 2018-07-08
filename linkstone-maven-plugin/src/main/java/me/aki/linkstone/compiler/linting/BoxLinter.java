package me.aki.linkstone.compiler.linting;

import me.aki.linkstone.annotations.Box;
import me.aki.linkstone.annotations.Boxed;
import me.aki.linkstone.compiler.meta.BoxMeta;
import me.aki.linkstone.compiler.meta.BoxedMeta;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.objectweb.asm.Opcodes.INVOKESPECIAL;

/**
 * Lint if a {@link Box} annotation has illegal types or the annotated class
 * has no corresponding constructor.
 */
public class BoxLinter implements Linter {
    @Override
    public void lint(List<ClassNode> classes, ErrorReport report) {
        Map<Type, Type> boxToBoxed = new HashMap<>();
        for (ClassNode cn : classes) {
            BoxMeta meta = BoxMeta.from(cn);
            if (!meta.isAnnotated()) {
                continue;
            }

            boxToBoxed.put(Type.getObjectType(cn.name), meta.getType());

            if (validateBoxType(cn, meta.getType(), report)) {
                checkBoxedField(cn, meta.getType(), report);
                lintMissingConstructor(cn, meta.getType(), report);
            }
        }

        for (ClassNode cn : classes) {
            for (MethodNode mn : cn.methods) {
                lintIllegalConstructorAccess(cn, mn, boxToBoxed, report);
            }
        }
    }

    /**
     * Lint if there's no field with a {@link Boxed} annotation that has the corrent type.
     *
     * @param cn current class being processed
     * @param boxType type that the class boxes
     * @param report error report for user
     */
    private void checkBoxedField(ClassNode cn, Type boxType, ErrorReport report) {
        List<FieldNode> annotatedFields = cn.fields.stream()
                .filter(fn -> BoxedMeta.from(fn).isAnnotated())
                .collect(Collectors.toList());

        switch (annotatedFields.size()) {
            case 0: {
                ErrorReport.Class location = new ErrorReport.Class(cn.name);
                String message = "Cannot find field with @Boxed annotation";
                report.addError(new ErrorReport.Error(message, location));
                break;
            }

            case 1: {
                FieldNode fn = annotatedFields.get(0);
                if (!fn.desc.equals(boxType.getDescriptor())) {
                    ErrorReport.Field location = new ErrorReport.Field(cn.name, fn.name, fn.desc);
                    String message = "@Boxed field has a different type than the @Box annotation";
                    report.addError(new ErrorReport.Error(message, location));
                }
                break;
            }

            default: {
                ErrorReport.Class location = new ErrorReport.Class(cn.name);
                String message = "Multiple fields have a @Boxed annotation";
                report.addError(new ErrorReport.Error(message, location));
                break;
            }
        }
    }

    private boolean validateBoxType(ClassNode cn, Type type, ErrorReport report) {
        if (type.getSort() == Type.OBJECT) {
            return true;
        } else {
            ErrorReport.Class location = new ErrorReport.Class(cn.name);
            String message = "Only class types can be boxed";
            report.addError(new ErrorReport.Error(message, location));
            return false;
        }
    }

    private void lintMissingConstructor(ClassNode cn, Type type, ErrorReport report) {
        boolean hasConstructor = cn.methods.stream().anyMatch(mn ->
                mn.name.equals("<init>") &&
                        mn.desc.equals("(" + type.getDescriptor() + ")V"));

        if (!hasConstructor) {
            ErrorReport.Class location = new ErrorReport.Class(cn.name);
            String message = "Cannot find box descriptor for type " + type.getInternalName();
            report.addError(new ErrorReport.Error(message, location));
        }
    }

    /**
     * Lint if a box class is initialize with the new keyword.
     *
     * @param cn current class being processed
     * @param mn current method being processed
     * @param boxToBoxed Map that maps boxed to their boxed type
     * @param report error report for the user
     */
    private void lintIllegalConstructorAccess(ClassNode cn, MethodNode mn, Map<Type, Type> boxToBoxed, ErrorReport report) {
        // a mutable integer
        int[] supercallCount = new int[] { 0 };

        mn.instructions.iterator().forEachRemaining(insn -> {
            MethodInsnNode min;
            if (insn.getOpcode() != INVOKESPECIAL ||
                    !((min = (MethodInsnNode) insn).name.equals("<init>"))) {
                return;
            }

            Type boxType = Type.getObjectType(min.owner);
            Type boxedType = boxToBoxed.get(boxType);
            Type[] arguments = Type.getArgumentTypes(min.desc);
            if (boxedType == null || arguments.length != 1 || !arguments[0].equals(boxedType)) {
                return;
            }

            if (mn.name.equals("<init>")) {
                // Every constructor must make a "super(...);" or "this(...);" call in its constructor.
                // These calls may look like a constructor invoke of a box.
                if (Type.getObjectType(cn.superName).equals(boxType) ||
                        Type.getObjectType(cn.name).equals(boxType)) {
                    supercallCount[0] += 1;
                    if (supercallCount[0] <= 1) {
                        return;
                    }
                }
            }

            ErrorReport.Method location = new ErrorReport.Method(cn.name, mn.name, mn.desc);
            String message = "Boxes should not be initialized with the new keyword. " +
                    "Use \"BoxCache.box(objectToBox, " + getSimpleName(boxType) + ".class)\" instead.";
            report.addError(new ErrorReport.Error(message, location));
        });
    }

    /**
     * Get the name of a class without the package.
     *
     * @param type a class
     * @return name of class
     * @see Class#getSimpleName()
     */
    private String getSimpleName(Type type) {
        String fullName = type.getInternalName();
        int lastSlashIndex = fullName.lastIndexOf('/');
        return lastSlashIndex < 0 ? fullName : fullName.substring(lastSlashIndex + 1);
    }
}
