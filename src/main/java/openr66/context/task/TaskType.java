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

import openr66.context.R66Session;
import openr66.context.task.exception.OpenR66RunnerErrorException;

/**
 * This enum class references all available tasks.
 *
 * If a new task type is to be created, this is the place where it should be referenced.
 *
 * @author Frederic Bregier
 *
 */
public enum TaskType {
    TEST, MOVE, MOVERENAME, COPY, COPYRENAME, EXEC, EXECRENAME;

    public int type;

    public String name;

    private TaskType() {
        type = ordinal();
        name = name();
    }
    /**
     *
     * @param argRule
     * @param delay
     * @param session
     * @param type
     * @return the corresponding AbstractTask
     * @throws OpenR66RunnerErrorException
     */
    public static AbstractTask getTaskFromId(String argRule, int delay,
            R66Session session, TaskType type) throws OpenR66RunnerErrorException {
        switch (type) {
            case TEST:
                return new TestTask(argRule, delay, session.getRunner()
                        .getFileInformation(), session);
            case MOVE:
                return new MoveTask(argRule, delay, session.getRunner()
                        .getFileInformation(), session);
            case MOVERENAME:
                return new MoveRenameTask(argRule, delay, session.getRunner()
                        .getFileInformation(), session);
            case COPY:
                return new CopyTask(argRule, delay, session.getRunner()
                        .getFileInformation(), session);
            case COPYRENAME:
                return new CopyRenameTask(argRule, delay, session.getRunner()
                        .getFileInformation(), session);
            case EXEC:
                return new ExecTask(argRule, delay, session.getRunner()
                        .getFileInformation(), session);
            case EXECRENAME:
                return new ExecRenameTask(argRule, delay, session.getRunner()
                        .getFileInformation(), session);
            default:
                throw new OpenR66RunnerErrorException("Unvalid Task: " +
                        type.name);
        }
    }
    /**
     *
     * @param name
     * @param argRule
     * @param delay
     * @param session
     * @return the corresponding AbstractTask
     * @throws OpenR66RunnerErrorException
     */
    public static AbstractTask getTaskFromId(String name, String argRule, int delay,
            R66Session session) throws OpenR66RunnerErrorException {
        TaskType type;
        try {
            type = valueOf(name);
        } catch (NullPointerException e) {
            System.err.println("name: "+name);
            throw new OpenR66RunnerErrorException("Unvalid Task: " +
                    name);
        }
        return getTaskFromId(argRule, delay, session, type);
    }
}
