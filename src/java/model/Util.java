package model;

public class Util {


    public static String generateCode() {
        int random = (int) (Math.random() * 100000);
        return String.format("%06d", random);
    }

    public static boolean isEmailValid(String email) {
        return email.matches("^[a-zA-Z0-9_!#$%&’*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$");
    }

    public static boolean isPasswordValid(String password) {
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=]).{8,}$");
    }

    public static boolean isCodeValid(String code) {
        return code.matches("^\\d{4,5}$");
    }

    public static boolean isInteger(String value) {
        return value.matches("^\\d+$");
    }

    public static boolean isDouble(String text) {
        return text.matches("^\\d+(\\.\\d{2})?$");
    }

    public static boolean isMobileValid(String mobile) {
        return mobile.matches("^07[0145678][0-9]{7}$");
    }
}
