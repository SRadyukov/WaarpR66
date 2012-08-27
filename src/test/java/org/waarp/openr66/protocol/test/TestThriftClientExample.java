/**
 * This file is part of Waarp Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Waarp . If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.openr66.protocol.test;

import java.util.List;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.waarp.thrift.r66.Action;
import org.waarp.thrift.r66.R66Request;
import org.waarp.thrift.r66.R66Result;
import org.waarp.thrift.r66.R66Service;
import org.waarp.thrift.r66.RequestMode;

/**
 * Example of Java class to interact with the Thrift R66 service
 * @author "Frederic Bregier"
 * 
 */
public class TestThriftClientExample  {
	private static final int PORT = 4266;
	private static final int tries = 100000;

	public static void main(String[] args) {
		try {
			TTransport transport = null;
			transport = new TSocket("localhost", PORT);
			TProtocol protocol = new TBinaryProtocol(transport);
			R66Service.Client client = new R66Service.Client(protocol);
			transport.open();
			R66Request request = new R66Request(RequestMode.INFOFILE);
			request.setDestuid("hostas");
			request.setFromuid("tests");
			request.setRule("rule3");
			request.setAction(Action.List);

			System.out.println("REQUEST1: " + request.toString());
			List<String> list = client.infoListQuery(request);
			System.out.println("RESULT1: " + list.size());
			for (String slist :list) {
				System.out.println(slist);
			}

			long start = System.currentTimeMillis();
			for (int i = 0; i < tries; i++) {
				list = client.infoListQuery(request);
			}
			long end = System.currentTimeMillis();
			System.out
					.println("Delay: " + (end - start) + " : " + ((tries * 1000) / (end - start)));


			long startEx = System.currentTimeMillis();
			boolean dontknow = false;
			for (int i = 0; i < tries; i++) {
				dontknow = client.isStillRunning("tests", "hostas", 1346080633424L);
			}
			long endEx = System.currentTimeMillis();
			System.out.println("StillRunning: "+dontknow);
			System.out
					.println("Delay: " + (endEx - startEx) + " : " + ((tries * 1000) / (endEx - startEx)));

			request.setMode(RequestMode.INFOREQUEST);
			request.setTid(1346080633424L);
			request.setAction(Action.Detail);
			R66Result result = client.infoTransferQuery(request);
			System.out.println("RESULT2: " + result.toString());
			long startQu = System.currentTimeMillis();
			for (int i = 0; i < tries; i++) {
				result = client.infoTransferQuery(request);
			}
			long endQu = System.currentTimeMillis();
			System.out
					.println("Delay: " + (endQu - startQu) + " : " + ((tries * 1000) / (endQu - startQu)));

			System.out.println("Exist: "
					+
					client.isStillRunning(request.getFromuid(), request.getDestuid(),
							request.getTid()));

			request.setMode(RequestMode.INFOFILE);
			request.setAction(Action.Mlsx);
			list = client.infoListQuery(request);
			System.out.println("RESULT3: " + list.size());
			for (String slist :list) {
				System.out.println(slist);
			}

			transport.close();
		} catch (TTransportException e) {
			e.printStackTrace();
		} catch (TException e) {
			e.printStackTrace();
		}
	}

}
