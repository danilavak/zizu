#include "pch.h"
#include "MainWindow.xaml.h"

#if __has_include("MainWindow.g.cpp")
#include "MainWindow.g.cpp"
#endif

#include "microsoft.ui.xaml.window.h"

namespace winrt
{
    using namespace Microsoft::UI::Xaml;
}

namespace winrt::ZizuWindowsClient::implementation
{
    MainWindow::MainWindow()
    {
        InitializeComponent();
        Title(L"Zizu Windows Client");
    }

    MainWindow::~MainWindow()
    {
        removeTrayIcon();
        if (m_hwnd != nullptr && m_previousWindowProc != nullptr)
        {
            SetWindowLongPtrW(m_hwnd, GWLP_WNDPROC, reinterpret_cast<LONG_PTR>(m_previousWindowProc));
        }
    }

    void MainWindow::InitializeWindow(bool startHidden)
    {
        Activate();

        m_hwnd = getWindowHandle();
        installWindowHook();

        SetWindowPos(m_hwnd, nullptr, 0, 0, 920, 560, SWP_NOMOVE | SWP_NOZORDER);
        m_taskbarCreatedMessage = RegisterWindowMessageW(L"TaskbarCreated");
        ensureTrayIcon();

        if (startHidden)
        {
            hideToTray();
            updateStatus(L"Запуск выполнен в скрытом режиме.");
        }
        else
        {
            showMainWindow();
        }
    }

    void MainWindow::OnExitMenuItemClick(
            winrt::Windows::Foundation::IInspectable const&,
            winrt::Microsoft::UI::Xaml::RoutedEventArgs const&)
    {
        exitApplication();
    }

    LRESULT CALLBACK MainWindow::WindowProc(HWND hwnd, UINT message, WPARAM wParam, LPARAM lParam)
    {
        auto* self = reinterpret_cast<MainWindow*>(GetWindowLongPtrW(hwnd, GWLP_USERDATA));
        if (self != nullptr)
        {
            return self->handleWindowMessage(message, wParam, lParam);
        }
        return DefWindowProcW(hwnd, message, wParam, lParam);
    }

    HWND MainWindow::getWindowHandle()
    {
        if (m_hwnd == nullptr)
        {
            winrt::Microsoft::UI::Xaml::Window window = *this;
            window.as<IWindowNative>()->get_WindowHandle(&m_hwnd);
        }
        return m_hwnd;
    }

    void MainWindow::installWindowHook()
    {
        SetWindowLongPtrW(m_hwnd, GWLP_USERDATA, reinterpret_cast<LONG_PTR>(this));
        m_previousWindowProc = reinterpret_cast<WNDPROC>(
                SetWindowLongPtrW(m_hwnd, GWLP_WNDPROC, reinterpret_cast<LONG_PTR>(&MainWindow::WindowProc)));
    }

    LRESULT MainWindow::handleWindowMessage(UINT message, WPARAM wParam, LPARAM lParam)
    {
        if (message == m_taskbarCreatedMessage)
        {
            ensureTrayIcon();
            return 0;
        }

        switch (message)
        {
        case WM_CLOSE:
            if (!m_isExiting)
            {
                hideToTray();
                updateStatus(L"Окно скрыто, приложение продолжает работать в фоне.");
                return 0;
            }
            break;
        case WM_TRAYICON:
            switch (LOWORD(lParam))
            {
            case WM_LBUTTONUP:
                showMainWindow();
                return 0;
            case WM_RBUTTONUP:
            case WM_CONTEXTMENU:
                showTrayMenu();
                return 0;
            default:
                break;
            }
            break;
        default:
            break;
        }

        return CallWindowProcW(m_previousWindowProc, m_hwnd, message, wParam, lParam);
    }

    void MainWindow::ensureTrayIcon()
    {
        ZeroMemory(&m_trayIconData, sizeof(m_trayIconData));
        m_trayIconData.cbSize = sizeof(m_trayIconData);
        m_trayIconData.hWnd = m_hwnd;
        m_trayIconData.uID = 1;
        m_trayIconData.uFlags = NIF_MESSAGE | NIF_ICON | NIF_TIP;
        m_trayIconData.uCallbackMessage = WM_TRAYICON;
        m_trayIconData.hIcon = LoadIconW(nullptr, IDI_APPLICATION);
        wcscpy_s(m_trayIconData.szTip, L"Zizu Windows Client");

        Shell_NotifyIconW(NIM_DELETE, &m_trayIconData);
        Shell_NotifyIconW(NIM_ADD, &m_trayIconData);
    }

    void MainWindow::removeTrayIcon()
    {
        if (m_trayIconData.hWnd != nullptr)
        {
            Shell_NotifyIconW(NIM_DELETE, &m_trayIconData);
        }
    }

    void MainWindow::showMainWindow()
    {
        ShowWindow(m_hwnd, SW_SHOW);
        SetForegroundWindow(m_hwnd);
        Activate();
        updateStatus(L"Окно открыто.");
    }

    void MainWindow::hideToTray()
    {
        ShowWindow(m_hwnd, SW_HIDE);
    }

    void MainWindow::showTrayMenu()
    {
        HMENU menu = CreatePopupMenu();
        if (menu == nullptr)
        {
            return;
        }

        AppendMenuW(menu, MF_STRING, ID_TRAY_OPEN, L"Открыть");
        AppendMenuW(menu, MF_SEPARATOR, 0, nullptr);
        AppendMenuW(menu, MF_STRING, ID_TRAY_EXIT, L"Выход");

        POINT cursor{};
        GetCursorPos(&cursor);
        SetForegroundWindow(m_hwnd);

        const UINT command = TrackPopupMenu(
                menu,
                TPM_LEFTALIGN | TPM_RIGHTBUTTON | TPM_RETURNCMD,
                cursor.x,
                cursor.y,
                0,
                m_hwnd,
                nullptr);

        DestroyMenu(menu);

        switch (command)
        {
        case ID_TRAY_OPEN:
            showMainWindow();
            break;
        case ID_TRAY_EXIT:
            exitApplication();
            break;
        default:
            break;
        }
    }

    void MainWindow::updateStatus(std::wstring_view message)
    {
        StatusTextBlock().Text(winrt::hstring(message));
    }

    void MainWindow::exitApplication()
    {
        m_isExiting = true;
        removeTrayIcon();
        Close();
    }
}
