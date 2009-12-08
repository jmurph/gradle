/*=============================================================================
                    AUTOMATED LOGIC CORPORATION
            Copyright (c) 1999 - 2009 All Rights Reserved
     This document contains confidential/proprietary information.
===============================================================================

   @(#)TestListener

   Author(s) jmurph
   $Log: $    
=============================================================================*/
package org.gradle.api.tasks.testing;

import junit.framework.AssertionFailedError;

import java.io.Serializable;

// todo: consider multithreading/multiprocess issues
// Teamcity has the concept of a "wave" of messages
// where each thread/process uses a unique wave id
public interface TestListener {
    public interface Suite extends Serializable
    {
        public String getName();
    }
    public interface Test extends Serializable
    {
        public String getName();
    }
    public enum ResultType { SUCCESS, FAILURE, ERROR, SKIPPED }
    public interface Result extends Serializable
    {
        public ResultType getResultType();
        public Throwable getError(); // throws exception if type != ERROR
        public Throwable getFailure(); // throws exception if type != FAILURE
    }

    void suiteStarting(Suite suite);
    void suiteFinished(Suite suite);
    void testStarting(Test test);
    void testFinished(Test test, Result result);
}

