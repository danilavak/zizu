#pragma once

#include "pch.h"
#include "MainWindow.g.h"

namespace winrt::ZizuWindowsClient::implementation
{
    struct MainWindow : MainWindowT<MainWindow>
    {
        MainWindow();
        ~MainWindow();

        void InitializeWindow(bool startHidden);
        void OnExitMenuItemClick(winrt::Windows::Foundation::IInspectable const&, winrt::Microsoft::UI::Xaml::RoutedEventArgs const&);

    private:
        static constexpr UINT WM_TRAYICON = WM_APP + 1;
        static constexpr UINT ID_TRAY_OPEN = 1001;
        static constexpr UINT ID_TRAY_EXIT = 1002;

        static LRESULT CALLBACK WindowProc(HWND hwnd, UINT message, WPARAM wParam, LPARAM lParam);

        HWND getWindowHandle();
        LRESULT handleWindowMessage(UINT message, WPARAM wParam, LPARAM lParam);
        void installWindowHook();
        void ensureTrayIcon();
        void removeTrayIcon();
        void showMainWindow();
        void hideToTray();
        void showTrayMenu();
        void updateStatus(std::wstring_view message);
        void exitApplication();

        HWND m_hwnd{ nullptr };
        WNDPROC m_previousWindowProc{ nullptr };
        UINT m_taskbarCreatedMessage{ 0 };
        bool m_isExiting{ false };
        NOTIFYICONDATAW m_trayIconData{};
    };
}

namespace winrt::ZizuWindowsClient::factory_implementation
{
    struct MainWindow : MainWindowT<MainWindow, implementation::MainWindow>
    {
    };
}
