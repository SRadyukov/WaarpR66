/**
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author
 * tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3.0 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package openr66.context.task;

import goldengate.common.command.exception.CommandAbstractException;
import goldengate.common.logging.GgInternalLogger;
import goldengate.common.logging.GgInternalLoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import openr66.context.ErrorCode;
import openr66.context.R66Session;
import openr66.protocol.configuration.Configuration;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

/**
 * Execute an external command and Rename the file (using the new name from the result).
 *
 * The move of the file (if any) should be done by the external command itself.
 *
 * @author Frederic Bregier
 *
 */
public class ExecMoveTask extends AbstractTask {
    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory
            .getLogger(ExecMoveTask.class);

    /**
     * @param argRule
     * @param delay
     * @param argTransfer
     * @param session
     */
    public ExecMoveTask(String argRule, int delay, String argTransfer,
            R66Session session) {
        super(TaskType.EXECMOVE, delay, argRule, argTransfer, session);
    }

    /*
     * (non-Javadoc)
     *
     * @see openr66.context.task.AbstractTask#run()
     */
    @Override
    public void run() {
        /*
         * First apply all replacements and format to argRule from context and
         * argTransfer. Will call exec (from first element of resulting string)
         * with arguments as the following value from the replacements. Return 0
         * if OK, else 1 for a warning else as an error. The last line of stdout
         * will be the new name given to the R66File in case of status 0. The
         * previous file should be deleted by the script or will be deleted in
         * case of status 0. If the status is 1, no change is made to the file.
         */
        logger.info("ExecRename with " + argRule + ":" + argTransfer + " and " +
                session);
        String finalname = argRule;
        finalname = getReplacedValue(finalname, argTransfer.split(" "));
        String[] args = finalname.split(" ");
        CommandLine commandLine = new CommandLine(args[0]);
        for (int i = 1; i < args.length; i ++) {
            commandLine.addArgument(args[i]);
        }
        DefaultExecutor defaultExecutor = new DefaultExecutor();
        PipedInputStream inputStream = new PipedInputStream();
        PipedOutputStream outputStream = null;
        try {
            outputStream = new PipedOutputStream(inputStream);
        } catch (IOException e1) {
            try {
                inputStream.close();
            } catch (IOException e) {
            }
            logger.error("Exception: " + e1.getMessage() +
                    " Exec in error with " + commandLine.toString(), e1);
            futureCompletion.setFailure(e1);
            return;
        }
        PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(
                outputStream, null);
        defaultExecutor.setStreamHandler(pumpStreamHandler);
        int[] correctValues = {
                0, 1 };
        defaultExecutor.setExitValues(correctValues);
        ExecuteWatchdog watchdog = null;
        if (delay > 0) {
            watchdog = new ExecuteWatchdog(delay);
            defaultExecutor.setWatchdog(watchdog);
        }
        LastLineReader lastLineReader = new LastLineReader(inputStream);
        Thread thread = new Thread(lastLineReader);
        thread.setDaemon(false);
        thread.setName("ExecRename" + session.getRunner().getSpecialId());
        thread.start();
        int status = -1;
        try {
            status = defaultExecutor.execute(commandLine);
        } catch (ExecuteException e) {
            try {
                outputStream.close();
            } catch (IOException e1) {
            }
            thread.interrupt();
            try {
                inputStream.close();
            } catch (IOException e1) {
            }
            pumpStreamHandler.stop();
            logger.error("Exception: " + e.getMessage() +
                    " Exec in error with " + commandLine.toString(), e);
            futureCompletion.setFailure(e);
            return;
        } catch (IOException e) {
            try {
                outputStream.close();
            } catch (IOException e1) {
            }
            thread.interrupt();
            try {
                inputStream.close();
            } catch (IOException e1) {
            }
            pumpStreamHandler.stop();
            logger.error("Exception: " + e.getMessage() +
                    " Exec in error with " + commandLine.toString(), e);
            futureCompletion.setFailure(e);
            return;
        }
        try {
            outputStream.flush();
        } catch (IOException e) {
        }
        try {
            outputStream.close();
        } catch (IOException e) {
        }
        pumpStreamHandler.stop();
        try {
            if (delay > 0) {
                thread.join(delay);
            } else {
                thread.join(Configuration.configuration.TIMEOUTCON);
            }
        } catch (InterruptedException e) {
        }
        try {
            inputStream.close();
        } catch (IOException e1) {
        }
        String newname = null;
        if (defaultExecutor.isFailure(status) && watchdog != null &&
                watchdog.killedProcess()) {
            // kill by the watchdoc (time out)
            status = -1;
            newname = "TimeOut";
        } else {
            newname = lastLineReader.lastLine;
            if (status == 0 && (newname == null || newname.length() == 0)) {
                status = 1;
            }
        }
        if (status == 0) {
            if (newname.indexOf(' ') > 0) {
                logger.warn("Exec returns a multiple string in final line: " +
                        newname);
                args = newname.split(" ");
                newname = args[args.length - 1];
            }
            // now test if the previous file was deleted (should be)
            File file = new File(newname);
            if (! file.exists()) {
                logger.warn("New file does not exist at the end of the exec: "+newname);
            }
            try {
                if (session.getFile().isFile()) {
                    // not deleted, so do it now
                    try {
                        session.getFile().delete();
                    } catch (CommandAbstractException e) {
                        logger.warn("Original File cannot be deleted", e);
                    }
                }
            } catch (CommandAbstractException e) {
            }
            // now replace the file with the new one
            try {
                session.getFile().replaceFilename(newname, true);
            } catch (CommandAbstractException e) {
                logger
                        .warn("Exec in warning with " + commandLine.toString(),
                                e);
            }
            session.getRunner().setFileMoved(true);
            futureCompletion.setSuccess();
            logger.info("Exec OK with " + commandLine.toString() + " returns " +
                    newname);
        } else if (status == 1) {
            logger.warn("Exec in warning with " + commandLine.toString() +
                    " returns " + newname);
            session.getRunner().setExecutionStatus(ErrorCode.Warning);
            futureCompletion.setSuccess();
        } else {
            logger.error("Status: " + status + " Exec in error with " +
                    commandLine.toString() + " returns " + newname);
            futureCompletion.cancel();
        }
    }
}