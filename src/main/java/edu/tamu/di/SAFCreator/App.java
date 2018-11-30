package edu.tamu.di.SAFCreator;

import edu.tamu.di.SAFCreator.controller.UserInterfaceController;
import edu.tamu.di.SAFCreator.model.importData.ImportDataProcessor;
import edu.tamu.di.SAFCreator.model.importData.ImportDataProcessorImpl;
import edu.tamu.di.SAFCreator.view.UserInterfaceView;

/**
 *
 */
public class App {
    private static ImportDataProcessor processor = new ImportDataProcessorImpl();

    private static UserInterfaceView view = new UserInterfaceView();

    @SuppressWarnings("unused")
    private static UserInterfaceController controller = new UserInterfaceController(processor, view);


    public static void main(String[] args) {
        // disable SNI, which must be set before any SSL connection is opened or it will not have any effect.
        // this avoids "SSL handshake alert: unrecognized_name".
        System.setProperty("jsse.enableSNIExtension", "false");

        view.setSize(800, 640);
        view.setResizable(false);
        view.setVisible(true);
    }
}
