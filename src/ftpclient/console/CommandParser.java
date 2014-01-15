/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ftpclient.console;

import ftpclient.FTPClient;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
 * @author sos
 */
public class CommandParser {
    public static void parseCommand(FTPClient client, String command) throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        switch (command) {
            case "ls":      client.ls(); break;
            case "pwd":     client.pwd(); break;
            case "quit":    client.disconnect(); break;
            case "abort":   client.abort(); break;
            case "noop":    client.noop(); break;
            default:
                String[] commands = {"ls ", "cd ", "rm ", "stor "};
                String[] result;
                boolean invalid = true;
                for (int i=0; i<commands.length; i++) {
                    if (command.startsWith(commands[i])) {
                        result = command.split("\\ ");
                        if (result.length > 1) {
                            Class klass = Class.forName("ftpclient.FTPClient");
                            Method method = klass.getMethod(result[0], String.class);
                            method.invoke(client, result[1]);
                            invalid = false;
                        }
                    }
                }
                if (invalid)
                    System.out.println("Invalid command");
                break;
        }
    }
}
