#include <shellapi.h>
#include <windows.h>

#include <memory>
#include <string>
#include <type_traits>

#include "../resources/resource.h"

namespace {
constexpr wchar_t kWindowClassName[] = L"ZizuTrayWindowClass";
constexpr wchar_t kWindowTitle[] = L"Zizu Windows Client";
constexpr wchar_t kMutexName[] = L"Local\\ZizuWindowsClientSingleInstance";
constexpr UINT kTrayIconId = 1;
constexpr UINT kTrayCallbackMessage = WM_APP + 1;

HINSTANCE g_instance = nullptr;
HWND g_window = nullptr;
HMENU g_mainMenu = nullptr;
UINT g_taskbarCreatedMessage = 0;
std::unique_ptr<std::remove_pointer_t<HANDLE>, decltype(&CloseHandle)> g_mutex(nullptr, CloseHandle);
bool g_exitRequested = false;

bool HasHiddenFlag() {
    int argc = 0;
    LPWSTR* argv = CommandLineToArgvW(GetCommandLineW(), &argc);
    if (argv == nullptr) {
        return false;
    }

    bool hidden = false;
    for (int index = 1; index < argc; ++index) {
        std::wstring argument = argv[index];
        if (argument == L"--hidden" || argument == L"/hidden" || argument == L"--background") {
            hidden = true;
            break;
        }
    }

    LocalFree(argv);
    return hidden;
}

HICON LoadTrayIcon() {
    return LoadIconW(nullptr, IDI_APPLICATION);
}

void FillTrayData(NOTIFYICONDATAW& trayData) {
    ZeroMemory(&trayData, sizeof(trayData));
    trayData.cbSize = sizeof(trayData);
    trayData.hWnd = g_window;
    trayData.uID = kTrayIconId;
    trayData.uFlags = NIF_MESSAGE | NIF_ICON | NIF_TIP;
    trayData.uCallbackMessage = kTrayCallbackMessage;
    trayData.hIcon = LoadTrayIcon();
    wcscpy_s(trayData.szTip, L"Zizu Windows Client");
}

bool AddTrayIcon() {
    NOTIFYICONDATAW trayData;
    FillTrayData(trayData);
    return Shell_NotifyIconW(NIM_ADD, &trayData) == TRUE;
}

void RemoveTrayIcon() {
    NOTIFYICONDATAW trayData;
    FillTrayData(trayData);
    Shell_NotifyIconW(NIM_DELETE, &trayData);
}

void RestoreMainWindow() {
    ShowWindow(g_window, SW_SHOW);
    ShowWindow(g_window, SW_RESTORE);
    SetForegroundWindow(g_window);
}

void HideMainWindow() {
    ShowWindow(g_window, SW_HIDE);
}

void ExitApplication() {
    g_exitRequested = true;
    DestroyWindow(g_window);
}

void ShowTrayMenu() {
    HMENU trayMenuRoot = LoadMenuW(g_instance, MAKEINTRESOURCEW(IDR_TRAY_MENU));
    if (trayMenuRoot == nullptr) {
        return;
    }

    HMENU trayMenu = GetSubMenu(trayMenuRoot, 0);
    if (trayMenu == nullptr) {
        DestroyMenu(trayMenuRoot);
        return;
    }

    POINT point{};
    GetCursorPos(&point);
    SetForegroundWindow(g_window);
    TrackPopupMenu(trayMenu, TPM_BOTTOMALIGN | TPM_LEFTALIGN | TPM_RIGHTBUTTON, point.x, point.y, 0, g_window, nullptr);
    PostMessageW(g_window, WM_NULL, 0, 0);
    DestroyMenu(trayMenuRoot);
}

LRESULT CALLBACK WindowProc(HWND window, UINT message, WPARAM wParam, LPARAM lParam) {
    if (message == g_taskbarCreatedMessage) {
        AddTrayIcon();
        return 0;
    }

    switch (message) {
        case WM_CREATE:
            g_window = window;
            AddTrayIcon();
            return 0;

        case WM_COMMAND:
            switch (LOWORD(wParam)) {
                case IDM_FILE_EXIT:
                case IDM_TRAY_EXIT:
                    ExitApplication();
                    return 0;
                case IDM_TRAY_OPEN:
                    RestoreMainWindow();
                    return 0;
                default:
                    break;
            }
            break;

        case WM_CLOSE:
            if (!g_exitRequested) {
                HideMainWindow();
                return 0;
            }
            break;

        case WM_DESTROY:
            RemoveTrayIcon();
            PostQuitMessage(0);
            return 0;

        case kTrayCallbackMessage:
            switch (LOWORD(lParam)) {
                case WM_LBUTTONUP:
                    RestoreMainWindow();
                    return 0;
                case WM_RBUTTONUP:
                case WM_CONTEXTMENU:
                    ShowTrayMenu();
                    return 0;
                default:
                    break;
            }
            break;

        default:
            break;
    }

    return DefWindowProcW(window, message, wParam, lParam);
}

bool RegisterWindowClass() {
    WNDCLASSEXW windowClass{};
    windowClass.cbSize = sizeof(windowClass);
    windowClass.lpfnWndProc = WindowProc;
    windowClass.hInstance = g_instance;
    windowClass.hIcon = LoadTrayIcon();
    windowClass.hCursor = LoadCursorW(nullptr, IDC_ARROW);
    windowClass.hbrBackground = reinterpret_cast<HBRUSH>(COLOR_WINDOW + 1);
    windowClass.lpszClassName = kWindowClassName;
    windowClass.hIconSm = LoadTrayIcon();
    return RegisterClassExW(&windowClass) != 0;
}

bool CreateSingleInstanceMutex() {
    HANDLE mutexHandle = CreateMutexW(nullptr, FALSE, kMutexName);
    if (mutexHandle == nullptr) {
        return false;
    }

    if (GetLastError() == ERROR_ALREADY_EXISTS) {
        CloseHandle(mutexHandle);
        return false;
    }

    g_mutex.reset(mutexHandle);
    return true;
}
}

int WINAPI wWinMain(HINSTANCE instance, HINSTANCE, PWSTR, int commandShow) {
    g_instance = instance;

    if (!CreateSingleInstanceMutex()) {
        return 0;
    }

    g_taskbarCreatedMessage = RegisterWindowMessageW(L"TaskbarCreated");

    if (!RegisterWindowClass()) {
        return 1;
    }

    g_mainMenu = LoadMenuW(g_instance, MAKEINTRESOURCEW(IDR_MAIN_MENU));

    HWND window = CreateWindowExW(
            0,
            kWindowClassName,
            kWindowTitle,
            WS_OVERLAPPEDWINDOW,
            CW_USEDEFAULT,
            CW_USEDEFAULT,
            900,
            600,
            nullptr,
            g_mainMenu,
            g_instance,
            nullptr);

    if (window == nullptr) {
        if (g_mainMenu != nullptr) {
            DestroyMenu(g_mainMenu);
        }
        return 1;
    }

    if (HasHiddenFlag()) {
        ShowWindow(window, SW_HIDE);
    } else {
        ShowWindow(window, commandShow == 0 ? SW_SHOWDEFAULT : commandShow);
        UpdateWindow(window);
    }

    MSG message{};
    while (GetMessageW(&message, nullptr, 0, 0) > 0) {
        TranslateMessage(&message);
        DispatchMessageW(&message);
    }

    if (g_mainMenu != nullptr) {
        DestroyMenu(g_mainMenu);
        g_mainMenu = nullptr;
    }

    return static_cast<int>(message.wParam);
}
