using System;
using System.Diagnostics;
using System.Runtime.InteropServices;

namespace TorchDesktop
{
    public class Log
    {
        private const string VERBOSE = "VERBOSE";
        private const string DEBUG = "DEBUG";
        private const string WARNING = "WARNING";
        private const string ERROR = "ERROR";
        private const string OK = "OK";

        static Log()
        {
            AllocConsole();
            Write("Log by iYehuda");
        }

        [DllImport("kernel32.dll", SetLastError = true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        static extern bool AllocConsole();

        private static void Write(string str)
        {
            Console.WriteLine(str);
        }

        private static void Write(string mode, string str)
        {
            Write($"[ {mode} ] {str}");
        }

        private static void Write(string mode, string tag, string str)
        {
            Write(mode, $"{tag}: {str}");
        }

        private static void Write(string mode, string tag, string str, Exception exception)
        {
            Write(tag, $"{str}\n{exception}");
        }

        public static void Verbose(string tag, string msg)
        {
            Write(VERBOSE, tag, msg);
        }

        public static void Verbose(string tag, string msg, Exception exception)
        {
            Write(VERBOSE, tag, msg, exception);
        }

        [Conditional("DEBUG")]
        public static void Debug(string tag, string msg)
        {
            Write(DEBUG, tag, msg);
        }

        [Conditional("DEBUG")]
        public static void Debug(string tag, string msg, Exception exception)
        {
            Write(DEBUG, tag, msg, exception);
        }

        public static void Warning(string tag, string msg)
        {
            Write(WARNING, tag, msg);
        }

        public static void Warning(string tag, string msg, Exception exception)
        {
            Write(WARNING, tag, msg, exception);
        }

        public static void Error(string tag, string msg)
        {
            Write(ERROR, tag, msg);
        }

        public static void Error(string tag, string msg, Exception exception)
        {
            Write(ERROR, tag, msg, exception);
        }

        public static void Ok(string tag, string msg)
        {
            Write(OK, tag, msg);
        }

        public static void Ok(string tag, string msg, Exception exception)
        {
            Write(OK, tag, msg, exception);
        }
    }
}
