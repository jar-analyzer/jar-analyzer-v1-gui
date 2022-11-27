package com.chaitin.jar.analyzer.core;

import org.objectweb.asm.Type;

import java.util.Objects;

public class MethodReference {
    private final ClassReference.Handle classReference;
    private final String name;
    private final String desc;
    private final boolean isStatic;

    public MethodReference(ClassReference.Handle classReference,
                           String name, String desc, boolean isStatic) {
        this.classReference = classReference;
        this.name = name;
        this.desc = desc;
        this.isStatic = isStatic;
    }

    public ClassReference.Handle getClassReference() {
        return classReference;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public Handle getHandle() {
        return new Handle(classReference, name, desc);
    }

    public static class Handle {
        private final ClassReference.Handle classReference;
        private final String name;
        private final String desc;

        public Handle(ClassReference.Handle classReference, String name, String desc) {
            this.classReference = classReference;
            this.name = name;
            this.desc = desc;
        }

        public ClassReference.Handle getClassReference() {
            return classReference;
        }

        public String getName() {
            return name;
        }

        public String getDesc() {
            return desc;
        }

        public String getDescStd() {
            Type methodType = Type.getMethodType(desc);
            StringBuilder sb = new StringBuilder();

            sb.append("return: ");
            sb.append(methodType.getReturnType().getClassName());
            sb.append("\n");

            Type[] argTypes = methodType.getArgumentTypes();
            sb.append("args num: ");
            sb.append(argTypes.length);
            sb.append("\n");
            for (int ix = 0; ix < argTypes.length; ix++) {
                sb.append("arg")
                        .append(ix).append(": ")
                        .append(argTypes[ix].getClassName());
                sb.append("\n");
            }
            sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Handle handle = (Handle) o;
            if (!Objects.equals(classReference, handle.classReference))
                return false;
            if (!Objects.equals(name, handle.name))
                return false;
            return Objects.equals(desc, handle.desc);
        }

        @Override
        public int hashCode() {
            int result = classReference != null ? classReference.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (desc != null ? desc.hashCode() : 0);
            return result;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodReference handle = (MethodReference) o;
        if (!Objects.equals(classReference, handle.classReference))
            return false;
        if (!Objects.equals(name, handle.name))
            return false;
        return Objects.equals(desc, handle.desc);
    }

    @Override
    public int hashCode() {
        int result = classReference != null ? classReference.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (desc != null ? desc.hashCode() : 0);
        return result;
    }
}
