package com.pixelchief.shizuku;

interface IShellService {
    void destroy() = 16777114;
    String execCommand(String cmd) = 1;
    int execInt(String cmd) = 2;
}
