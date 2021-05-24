package RRFieldCodes;

public enum RRType {  // Типы из формулировки задачи
    A(1),
    AAAA(28),
    NS(2),
    PTR(12);

    private final int m_value;

    RRType(int value) {
        m_value = value;
    }

    public int getValue() { return m_value; }
}
