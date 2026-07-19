import Controllers.ControladorCliente;
import Views.theme.Theme;

public class Main {
    public static void main(String[] args) {
        Theme.install(Theme.Mode.LIGHT);
        // Arranca la lógica limpia del cliente
        new ControladorCliente();
    }
}