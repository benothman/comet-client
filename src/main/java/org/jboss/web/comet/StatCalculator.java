/*
 * Copyright 2012, Nabil Benothman, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.web.comet;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Set;

/**
 * {@code StatCalculator}
 * <p/>
 *
 * Created on May 3, 2012 at 11:42:28 AM
 *
 * @author <a href="mailto:nabil.benothman@gmail.com">Nabil Benothman</a>
 */
public class StatCalculator {

    /**
     * Create a new instance of {@code StatCalculator}
     */
    public StatCalculator() {
        super();
    }

    /**
     *
     * @param args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java " + StatCalculator.class.getName() + " path");
            System.exit(1);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(args[0])));
        HashMap<Integer, Pair> stats = new HashMap<Integer, Pair>();
        String line = null;

        while ((line = br.readLine()) != null) {
            if ("".equals(line.trim())) {
                continue;
            }

            String tab[] = line.split("\\s+");
            int key = Integer.parseInt(tab[0]);
            double value = Double.parseDouble(tab[1]);
            if (stats.get(key) == null) {
                stats.put(key, new Pair());
            }

            Pair p = stats.get(key);
            p.add(value);
        }
        br.close();

        Set<Integer> keys = stats.keySet();
        Pair p = null;
        for (int key : keys) {
            p = stats.get(key);
            System.out.println("  " + key + " \t " + p.getAvg());
        }
    }

    private static class Pair {

        private int counter = 0;
        private double sum = 0;

        double getAvg() {
            return sum / counter;
        }

        void add(double value) {
            sum += value;
            counter++;
        }
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     */
    private static class Tuple<A, B, C> {

        protected A first;
        protected B second;
        protected C third;
    }
}
