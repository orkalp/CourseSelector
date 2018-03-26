public class CourseNode {
    String code;
    String name;
    String professor;
    String hours;
    String availabilityString;
    int mustQuota;
    int electiveQuota;
    int mergedQuota;

    CourseNode(String code) {
        this.code = code;
    }

    CourseNode(String code, String name) {
        this.code = code;
        this.name = name;
        this.professor = "";
        this.hours = "";
    }

    CourseNode(String code, String name, String professor) {
        this.code = code;
        this.name = name;
        this.professor = professor;
        this.hours = "";
    }

    CourseNode(String code, String name, String professor, String hours) {
        this.code = code;
        this.name = name;
        this.professor = professor;
        this.hours = hours;
    }

    CourseNode(String code, String name, String professor, String hours, String availabilityString) {
        this.code = code;
        this.name = name;
        this.professor = professor;
        this.hours = hours;
        this.availabilityString = availabilityString;
    }

    CourseNode(String code, String name, String professor, String hours, String availabilityString, int must, int elect, int merged) {
        this.code = code;
        this.name = name;
        this.professor = professor;
        this.hours = hours;
        this.availabilityString = availabilityString;
        this.mustQuota = must;
        this.electiveQuota = elect;
        this.mergedQuota = merged;
    }

    public void printNode() {
        System.out.println(String.valueOf(this.code) + "  " + this.name + "  " + this.professor + "  " + this.hours);
    }
}
